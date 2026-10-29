package com.moamu.domain.user.service;

import com.moamu.domain.user.dto.UserSignupRequest;

public interface UserService {

  Long signup(UserSignupRequest Request);
}