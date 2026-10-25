package com.moamu.domain.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.moamu.domain.schedule.entity.Schedule;

public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    
}
