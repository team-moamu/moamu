package com.moamu.domain.user.repository;

/**
 * UserQueryRepository
 * QueryDSL 복잡한 조회 -> 기본 CRUD만 있다면 UserRepository.java에 작성하고 이 파일은 삭제해주세요.
 * 
 * @Repository: 조회 전용 Repository를 Spring Bean으로 등록
 * @RequiredArgsConstructor: JPAQueryFactory 생성자 주입
 */

@Repository
@RequiredArgsConstructor
public class UserQueryRepository {

    private final JPAQueryFactory queryFactory;

}