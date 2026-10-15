package com.moamu.domain.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moamu.domain.user.entity.User;

/**
 * UserRepository
 * JPA CRUD
 */


public interface UserRepository extends JpaRepository<User, Long> {
  
}