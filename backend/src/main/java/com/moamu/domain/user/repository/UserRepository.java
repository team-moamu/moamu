package com.moamu.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moamu.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
      
  boolean existsByLoginId(String loginId);

  Optional<User> findByLoginId(String loginId);
}