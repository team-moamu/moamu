package com.moamu.domain.user.exception;

import org.springframework.http.HttpStatus;

import com.moamu.global.exception.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements ErrorCode{

  DUPLICATE_LOGIN_ID(
    HttpStatus.CONFLICT,
    "DUPLICATE_LOGIN_ID",
    "이미 사용 중인 이메일입니다."
  );

  private final HttpStatus httpStatus;
  private final String code;
  private final String message;
  
}