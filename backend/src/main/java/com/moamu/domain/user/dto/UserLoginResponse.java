package com.moamu.domain.user.dto;

import lombok.Getter;

@Getter
public class UserLoginResponse {

  // 이후 인증이 필요한 요청에 사용할 JWT
  private final String accessToken;

  // Authorization 헤더에서 사용할 인증 방식
  private final String tokenType;

  public UserLoginResponse(String accessToken) {
    this.accessToken = accessToken;
    this.tokenType = "Bearer";
  }
}