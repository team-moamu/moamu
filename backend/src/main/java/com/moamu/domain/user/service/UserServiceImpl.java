package com.moamu.domain.user.service;

import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.moamu.domain.user.dto.UserSignupRequest;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.exception.UserErrorCode;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.exception.BusinessException;

@Service
@RequiredArgsConstructor 
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override 
    @Transactional 
    public Long signup(UserSignupRequest request){
        
        if(userRepository.existsByLoginId(request.getLoginId())){
            throw new BusinessException(UserErrorCode.DUPLICATE_LOGIN_ID);
        }
        
        String encodePassword = passwordEncoder.encode(request.getPassword());
        
        User user = User.createLocal(request.getLoginId(), encodePassword, request.getName());

        User savedUser = userRepository.save(user);

        return savedUser.getUserId();
        
        
    }
}