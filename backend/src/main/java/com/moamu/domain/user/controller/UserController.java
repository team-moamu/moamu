package com.moamu.domain.user.controller;

import com.moamu.domain.user.service.UserService;

/**
 * UserController
 * API 요청 처리
 * 
 * @RestController: REST API 컨트롤러로 등록
 * @RequestMapping: API 공통 경로 지정
 * @RequiredArgsConstructor: final 필드를 주입받는 생성자 자동 생성
 */

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
  
  private final UserService userService;
}
