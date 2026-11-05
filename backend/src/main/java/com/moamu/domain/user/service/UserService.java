package com.moamu.domain.user.service;

import com.moamu.domain.user.dto.UserLoginRequest;
import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.entity.User;

public interface UserService {

  Long signup(UserSignupRequest Request);

  //이메일의 사용 가능 여부 확인 (이미 가입되어 있는가)
  boolean isEmailAvailable(String email);

  User login(UserLoginRequest request);
}