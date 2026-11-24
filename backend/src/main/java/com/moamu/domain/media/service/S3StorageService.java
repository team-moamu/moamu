package com.moamu.domain.media.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

// 개발/배포 환경에서는 실제 S3를 사용
// 테스트에서는 Mock을 사용하여 S3에 미접근
@Service
@Profile({ "local", "prod" })
public class S3StorageService implements StorageService {
    private final S3Client s3Client;
    private final String bucket;
    private final String verificationPrefix;
    private final String profilePrefix;
    private final ImageValidator imageValidator = new ImageValidator();
    private final ImagePreprocessor imagePreprocessor;

    // S3 버킷 이름, S3 경로 확인
    public S3StorageService(
            S3Client s3Client,
            @Value("${app.storage.s3.bucket}") String bucket,
            @Value("${app.storage.s3.verification-prefix}") String verificationPrefix,
            @Value("${app.storage.s3.profile-prefix}") String profilePrefix,
            ImagePreprocessor imagePreprocessor) {
        this.imagePreprocessor = imagePreprocessor;

        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 버킷 이름이 필요합니다.");
        }

        this.s3Client = s3Client;
        this.bucket = bucket.trim();
        this.verificationPrefix = normalizePrefix(verificationPrefix);
        this.profilePrefix = normalizePrefix(profilePrefix);
    }

    private static String normalizePrefix(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("사진 저장 경로가 필요합니다.");
        }

        String prefix = value.trim();
        if (prefix.startsWith("/") || prefix.contains("\\")
                || java.util.Arrays.stream(prefix.split("/", -1))
                        .anyMatch(part -> part.equals(".") || part.equals(".."))) {
            throw new IllegalArgumentException("사진 저장 경로가 올바르지 않습니다.");
        }

        return prefix.endsWith("/") ? prefix : prefix + "/";
    }

    // ImageValidator 검증 (사진 검증 규칙)
    @Override
    public StoredFile saveVerification(MultipartFile image) {
        return saveImage(image, verificationPrefix);
    }

    @Override
    public StoredFile saveProfile(MultipartFile image) {
        return saveImage(image, profilePrefix);
    }

    private StoredFile saveImage(MultipartFile image, String prefix) {
        try (var validated = imageValidator.validate(image)) {
            byte[] processed = imagePreprocessor.process(validated);
            String imageKey = prefix + UUID.randomUUID() + "." + validated.extension();

            var request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(imageKey)
                    .contentType(validated.contentType())
                    .build();

            // 방향을 보정하고 크기를 줄인 이미지를 S3에 업로드
            s3Client.putObject(request, RequestBody.fromBytes(processed));

            // S3 객체 키와 원본 해시, 처리된 파일 정보를 반환
            return new StoredFile(
                    imageKey, validated.fileHash(), validated.contentType(),
                    (long) processed.length);
        }
    }

    @Override
    public void delete(String imageKey) {
        deleteImage(imageKey, verificationPrefix);
    }

    @Override
    public void deleteProfile(String imageKey) {
        deleteImage(imageKey, profilePrefix);
    }

    private void deleteImage(String imageKey, String prefix) {
        // 1. 인증 사진 저장 경로에 해당하는지 검증
        if (imageKey == null || !imageKey.startsWith(prefix)) {
            throw new IllegalArgumentException("허용되지 않은 S3 파일 경로입니다.");
        }
        // 2. 파일명에 잘못된 경로 요소가 포함되어 있는지 검증
        String filename = imageKey.substring(prefix.length());
        if (filename.isBlank() || filename.contains("/") || filename.contains("\\")
                || filename.equals(".") || filename.equals("..")) {
            throw new IllegalArgumentException("허용되지 않은 S3 파일 경로입니다.");
        }
        // 3. S3 객체 삭제 (실패 시 예외를 호출자에게 전파)
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .build());
    }
}
