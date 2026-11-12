package com.moamu.domain.media.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.moamu.domain.media.entity.Media;

// Media 엔티티 DB 접근 담당 Repository
public interface MediaRepository extends JpaRepository<Media, Long>{
    
}
