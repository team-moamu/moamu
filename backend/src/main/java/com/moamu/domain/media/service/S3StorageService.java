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
@Profile({"local", "prod"})
public class S3StorageService implements StorageService {
    private final S3Client s3Client;
    private final String bucket;
    private final String verificationPrefix;
    private final ImageValidator imageValidator = new ImageValidator();

    // S3 버킷 이름, S3 경로 확인
    public S3StorageService(
            S3Client s3Client,
            @Value("${app.storage.s3.bucket}") String bucket,
            @Value("${app.storage.s3.verification-prefix}") String verificationPrefix) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 버킷 이름이 필요합니다.");
        }
        if (verificationPrefix == null || verificationPrefix.isBlank()) {
            throw new IllegalArgumentException("인증 사진 S3 경로가 필요합니다.");
        }
        String prefix = verificationPrefix.trim();
        if (prefix.startsWith("/") || prefix.contains("\\")
                || java.util.Arrays.stream(prefix.split("/", -1))
                        .anyMatch(part -> part.equals(".") || part.equals(".."))) {
            throw new IllegalArgumentException("인증 사진 S3 경로가 올바르지 않습니다.");
        }
        this.s3Client = s3Client;
        this.bucket = bucket.trim();
        this.verificationPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
    }

    // ImageValidator 검증 (사진 검증 규칙)
    @Override
    public StoredFile saveVerification(MultipartFile image) {
        var validated = imageValidator.validate(image);
        String imageKey = verificationPrefix + UUID.randomUUID() + "." + validated.extension();
        var request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(imageKey)
                .contentType(validated.contentType())
                .build();

        // 검증된 이미지 데이터를 S3에 업로드 (공개 ACL 미설정)
        // 업로드 실패 시 예외를 전파하여 DB 성공 처리 방지
        s3Client.putObject(request, RequestBody.fromBytes(validated.bytes()));

        // S3 객체 키와 파일 정보를 반환 (DB 저장은 MediaService에서 처리)
        return new StoredFile(imageKey, validated.fileHash(), validated.contentType(),
                (long) validated.bytes().length);
    }

    @Override
    public void delete(String imageKey) {

        // 1. 인증 사진 저장 경로에 해당하는지 검증
        if (imageKey == null || !imageKey.startsWith(verificationPrefix)) {
            throw new IllegalArgumentException("허용되지 않은 S3 파일 경로입니다.");
        }
        // 2. 파일명에 잘못된 경로 요소가 포함되어 있는지 검증
        String filename = imageKey.substring(verificationPrefix.length());
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
