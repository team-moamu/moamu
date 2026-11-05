package com.moamu.domain.user.exception;

import org.springframework.http.HttpStatus;

import com.moamu.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode {

  // 이미 등록된 이메일로 회원가입 요청 한 경우
  DUPLICATE_LOGIN_ID(
      HttpStatus.CONFLICT,
      "DUPLICATE_LOGIN_ID",
      "이미 사용 중인 이메일입니다."
    ),

  // 이메일이 없거나 비밀번호가 일치하지 않는 경우
  INVALID_CREDENTIALS(
      HttpStatus.UNAUTHORIZED,
      "INVALID_CREDENTIALS",
      "이메일 또는 비밀번호가 올바르지 않습니다."
    ),

  // 탈퇴 등으로 로그인할 수 없는 경우
  LOGIN_NOT_ALLOWED(
      HttpStatus.FORBIDDEN,
      "LOGIN_NOT_ALLOWED",
      "로그인할 수 없는 회원입니다."
    );

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;

}