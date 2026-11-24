package com.moamu.domain.user.service;

import java.util.Locale;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.moamu.domain.user.dto.UserLoginRequest;
import com.moamu.domain.user.dto.UserMeResponse;
import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.exception.UserErrorCode;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.domain.media.enums.MediaPurpose;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.verification.service.ArtworkImageUrlService;
import com.moamu.global.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final SignupVerificationTokenStore signupVerificationTokenStore;
	private final MediaRepository mediaRepository;
	private final ArtworkImageUrlService artworkImageUrlService;

	@Override
	@Transactional
	public Long signup(UserSignupRequest request) {
		// 입력한 이메일의 대소문자를 유지해서 중복을 확인한다.
		String email = request.getLoginId();
		if (userRepository.existsByLoginId(email)) {
			throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
		}

		String verificationToken = request.getVerificationToken();
		if (!signupVerificationTokenStore.matchesEmail(verificationToken, email)) {
			throw new BusinessException(UserErrorCode.INVALID_EMAIL_VERIFICATION_TOKEN);
		}

		String encodedPassword = passwordEncoder.encode(request.getPassword());

		// 가입 시 기본 닉네임을 생성하고, 이후 마이페이지에서 변경한다.
		String nickname = "뮤_" + UUID.randomUUID()
				.toString()
				.replace("-", "")
				.substring(0, 12);

		User user = User.createLocal(email, encodedPassword, nickname);

		try {
			User savedUser = userRepository.saveAndFlush(user);
			deleteVerificationTokenAfterCommit(verificationToken);
			return savedUser.getUserId();
		} catch (DataIntegrityViolationException exception) {
			if (isDuplicateLoginId(exception)) {
				throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
			}

			throw exception;
		}
	}

	private void deleteVerificationTokenAfterCommit(String token) {
		// DB 커밋이 성공한 뒤에 토큰을 폐기해 저장 실패 시 재시도할 수 있게 한다.
		TransactionSynchronizationManager.registerSynchronization(
				new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						try {
							signupVerificationTokenStore.delete(token);
						} catch (DataAccessException exception) {
							log.warn("회원가입 완료 후 이메일 인증 토큰을 삭제하지 못했습니다.");
						}
					}
				});
	}

	@Override
	public boolean isEmailAvailable(String email) {
		return !userRepository.existsByLoginId(email);
	}

	private boolean isDuplicateLoginId(Throwable exception) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause instanceof ConstraintViolationException violation) {
				String constraintName = violation.getConstraintName();

				if (constraintName != null) {
					String normalizedName = constraintName
							.replace("`", "")
							.replace("'", "")
							.replace("\"", "");

					if (normalizedName.equalsIgnoreCase("uk_users_login_id")
							|| normalizedName.toLowerCase(Locale.ROOT)
									.endsWith(".uk_users_login_id")) {
						return true;
					}
				}
			}

			cause = cause.getCause();
		}

		return false;
	}

	@Override
	public User login(UserLoginRequest request) {
		String email = request.getLoginId();

		User user = userRepository.findByLoginId(email)
				.orElseThrow(() -> new BusinessException(UserErrorCode.INVALID_CREDENTIALS));

		// 비밀번호가 없거나 일치하지 않으면 로그인을 거절한다.
		if (user.getPassword() == null
				|| !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
			throw new BusinessException(UserErrorCode.INVALID_CREDENTIALS);
		}

		// 활성 회원만 로그인을 허용한다.
		if (user.getStatus() != UserStatus.ACTIVE) {
			throw new BusinessException(UserErrorCode.LOGIN_NOT_ALLOWED);
		}

		return user;
	}

	@Override
	public UserMeResponse getMe(Long userId) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		return toMeResponse(user);
	}

	@Override
	@Transactional
	public UserMeResponse updateMyName(Long userId, String name) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

		user.changeName(name.strip());

		return toMeResponse(user);
	}

	private UserMeResponse toMeResponse(User user) {
		Long profileMediaId = user.getProfileMediaId();
		String profilePhotoUrl = null;

		if (profileMediaId != null) {
			profilePhotoUrl = mediaRepository.findById(profileMediaId)
					.filter(media -> user.getUserId().equals(media.getUserId()))
					.filter(media -> media.getPurpose() == MediaPurpose.PROFILE)
					.map(media -> artworkImageUrlService.resolve(media.getImageKey()))
					.orElse(null);
		}

		return new UserMeResponse(user.getLoginId(), user.getName(), profilePhotoUrl);
	}
}