package com.moamu.domain.media.service;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    // 사진을 저장하고 파일 정보 반환
    StoredFile saveVerification(MultipartFile image);

    // DB 저장 실패 시 파일 삭제
    void delete(String imageKey);

    // 반환값
    record StoredFile(String imageKey, String fileHash, String contentType, Long fileSize){};

}