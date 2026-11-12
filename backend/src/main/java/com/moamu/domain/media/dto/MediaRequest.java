package com.moamu.domain.media.dto;

import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.Setter;

// 인증 사진 업로드 요청 dto
@Getter
@Setter
public class MediaRequest {
    private Long goalId;
    private MultipartFile image;
}
