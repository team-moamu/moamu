package com.moamu.domain.media.entity;

import java.time.LocalDateTime;

import com.moamu.domain.media.enums.MediaPurpose;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity 
@Table (name = "media")
@Getter 
@NoArgsConstructor (access = AccessLevel.PROTECTED)
public class Media {
    
    // 미디어 ID
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    @Column(name = "media_id")
    private Long mediaId;

    // 로컬/S3 파일 저장 위치
    @Column(name = "image_key", nullable = false, length = 500)
    private String imageKey;

    // SHA-256 파일 해시
    @Column(name = "file_hash", columnDefinition = "CHAR(64)")
    private String fileHash;

    // 실제 파일 형식
    @Column(name = "content_type", length = 100)
    private String contentType;

    // 파일 크기 (Byte)
    @Column(name = "file_size")
    private Long fileSize;

    // 등록 일시
    @Column(name = "frst_reg_dt", updatable = false)
    private LocalDateTime frstRegDt;

    // 업로드한 사용자 ID
    @Column(name = "user_id")
    private Long userId;

    // 사진 사용 목적
    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private MediaPurpose purpose;

    // 최초 저장 시 등록 일시 자동 설정
    @PrePersist
    private void onCreate() {
        if (frstRegDt == null) {
            frstRegDt = LocalDateTime.now();
        }
    }

    // 인증 사진 업로드 성공 시 Media 엔티티 생성
    // 업로드 상태는 SUCCESS
    // 사용 목적은 VERIFICATION으로 설정
    public static Media createVerification(
        String imageKey,
        String fileHash,
        String contentType,
        Long fileSize,
        Long userId
    ) {
        Media media = new Media();

        media.imageKey = imageKey;
        media.fileHash = fileHash;
        media.contentType = contentType;
        media.fileSize = fileSize;
        media.userId = userId;
        media.purpose = MediaPurpose.VERIFICATION;

        return media;
    }
    
}