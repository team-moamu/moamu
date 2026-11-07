package com.moamu.domain.verification.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moamu.domain.verification.entity.Verification;

public interface VerificationRepository extends JpaRepository<Verification, Long> {
    Optional<Verification> findByIdempotencyKey(String idempotencyKey);
}
