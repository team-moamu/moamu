package com.moamu.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moamu.domain.user.repository.UserRepository;

@Service
@RequiredArgsConstructor 
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    // 개발할때는 주석 풀어주세요
    // private final UserRepository userRepository;

}