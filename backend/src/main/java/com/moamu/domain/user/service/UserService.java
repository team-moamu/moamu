package com.moamu.domain.user.service;

/**
 * UserService
 * 비즈니스 로직
 * 
 * @Service: 비즈니스 로직 담당 Spring Bean
 * @RequiredArgsConstructor: 생성자 자동 생성
 * @Transactional(readOnly = true): 클래스의 조회 메서드에 읽기 전용 트랜잭션 기본 적용
 * @Transactional: 저장·수정·삭제 메서드에 별도로 적용
 */

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public interface UserService {

  
}