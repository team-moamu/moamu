package com.moamu.domain.media.dto;

import com.moamu.domain.media.enums.MediaStatus;

import lombok.Getter;
import lombok.Setter;

// 인증 사진 업로드 응답 dto
@Getter
@Setter
public class MediaResponse {
    private Long verificationId;
    private Long mediaId;
    private MediaStatus status; 
}
