package com.moamu.domain.verification;

import org.springframework.stereotype.Service;

import com.moamu.domain.schedule.repository.ScheduleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor 
public class VerificationService {
  private final ScheduleRepository scheduleRepository;

  
}
