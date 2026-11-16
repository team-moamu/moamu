package com.moamu.domain.user.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.moamu.domain.user.exception.UserErrorCode;
import com.moamu.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private static final Duration CODE_TTL = Duration.ofMinutes(5);
  private static final Duration RESEND_INTERVAL = Duration.ofSeconds(60);
  private static final Duration VERIFICATION_TOKEN_TTL = Duration.ofMinutes(10);
  private static final int MAX_FAILED_ATTEMPTS = 5;
  private static final SecureRandom RANDOM = new SecureRandom();

  private static final DefaultRedisScript<Long> DELETE_IF_MATCHES = new DefaultRedisScript<>("""
      if redis.call('GET', KEYS[1]) == ARGV[1] then
          return redis.call('DEL', KEYS[1])
      end
      return 0
      """, Long.class);

  // 조회한 인증번호가 여전히 유효할 때만 토큰 발급 또는 실패 횟수 갱신을 처리한다.
  private static final DefaultRedisScript<Long> VERIFY_CODE = new DefaultRedisScript<>("""
      local currentCode = redis.call('GET', KEYS[1])
      if currentCode ~= ARGV[1] then
          return 0
      end

      if ARGV[2] == 'true' then
          local saved = redis.call(
              'SET', KEYS[3], ARGV[3], 'PX', ARGV[4], 'NX')
          if not saved then
              return -2
          end
          redis.call('DEL', KEYS[1], KEYS[2])
          return 1
      end

      local ttl = redis.call('PTTL', KEYS[1])
      if ttl <= 0 then
          return 0
      end

      local attempts = redis.call('INCR', KEYS[2])
      if attempts >= tonumber(ARGV[5]) then
          redis.call('DEL', KEYS[1], KEYS[2])
          return -1
      end

      if attempts == 1 then
          redis.call('PEXPIRE', KEYS[2], ttl)
      end
      return 0
      """, Long.class);

  private final UserService userService;
  private final StringRedisTemplate redisTemplate;
  private final PasswordEncoder passwordEncoder;
  private final ObjectProvider<JavaMailSender> mailSenderProvider;

  @Value("${MAIL_FROM:}")
  private String fromAddress;

  public void sendCode(String email) {
    if (!userService.isEmailAvailable(email)) {
      throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
    }

    JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
    if (mailSender == null || fromAddress.isBlank()) {
      throw new BusinessException(UserErrorCode.VERIFICATION_MAIL_FAILED);
    }

    String cooldownKey = "auth:signup:cooldown:" + email.toLowerCase(Locale.ROOT);
    Boolean canSend = redisTemplate.opsForValue()
        .setIfAbsent(cooldownKey, "1", RESEND_INTERVAL);

    if (!Boolean.TRUE.equals(canSend)) {
      throw new BusinessException(UserErrorCode.VERIFICATION_RESEND_TOO_SOON);
    }

    String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
    String encodedCode = passwordEncoder.encode(code);

    String codeKey = "auth:signup:code:" + email;
    redisTemplate.opsForValue().set(codeKey, encodedCode, CODE_TTL);

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(email);
    message.setSubject("[모아뮤] 회원가입 인증번호");
    message.setText("인증번호는 " + code + "입니다.\n5분 안에 입력해 주세요.");

    try {
      mailSender.send(message);
    } catch (MailException exception) {
      // 발송에 실패한 요청의 인증번호만 삭제한다.
      redisTemplate.execute(
          DELETE_IF_MATCHES, List.of(codeKey), encodedCode);
      throw new BusinessException(UserErrorCode.VERIFICATION_MAIL_FAILED);
    }
  }

  public String verifyCode(String email, String code) {
    String codeKey = "auth:signup:code:" + email;
    String encodedCode = redisTemplate.opsForValue().get(codeKey);

    if (encodedCode == null) {
      throw new BusinessException(UserErrorCode.INVALID_VERIFICATION_CODE);
    }

    boolean matches = passwordEncoder.matches(code, encodedCode);

    byte[] tokenBytes = new byte[32];
    RANDOM.nextBytes(tokenBytes);
    String token = Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(tokenBytes);

    Long result = redisTemplate.execute(
        VERIFY_CODE,
        List.of(
            codeKey,
            "auth:signup:attempts:" + email + ":" + encodedCode,
            SignupVerificationTokenStore.key(token)),
        encodedCode,
        Boolean.toString(matches),
        email,
        Long.toString(VERIFICATION_TOKEN_TTL.toMillis()),
        Integer.toString(MAX_FAILED_ATTEMPTS));

    if (Long.valueOf(1).equals(result)) {
      return token;
    }

    if (Long.valueOf(-1).equals(result)) {
      throw new BusinessException(UserErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED);
    }

    if (Long.valueOf(0).equals(result)) {
      throw new BusinessException(UserErrorCode.INVALID_VERIFICATION_CODE);
    }

    throw new IllegalStateException("가입용 이메일 인증 토큰을 저장하지 못했습니다.");
  }
}