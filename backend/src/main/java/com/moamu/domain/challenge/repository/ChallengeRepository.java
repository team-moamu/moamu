package com.moamu.domain.challenge.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.moamu.domain.challenge.entity.Challenge;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
    
}
