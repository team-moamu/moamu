package com.moamu.domain.verification.service;

import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class ArtworkImageUrlService {

    private static final Duration IMAGE_URL_VALIDITY = Duration.ofMinutes(15);

    private final ObjectProvider<S3Presigner> presigners;
    private final Environment environment;

    public ArtworkImageUrlService(ObjectProvider<S3Presigner> presigners,
            Environment environment) {
        this.presigners = presigners;
        this.environment = environment;
    }

    // 작품 조회용 서명 URL 생성. 미디어가 없거나 S3 미사용 환경이면 null 반환
    public String resolve(String imageKey) {
        // 작품 미디어가 아직 연결되지 않은 경우 이미지 없이 응답
        if (imageKey == null || imageKey.isBlank()) {
            return null;
        }

        // S3를 사용하지 않는 테스트 환경에서는 서명 URL을 생성하지 않음
        S3Presigner presigner = presigners.getIfAvailable();
        if (presigner == null) {
            return null;
        }

        // 서명에 필요한 버킷 설정은 실제 S3를 사용하는 경우에만 조회
        String bucket = environment.getRequiredProperty("app.storage.s3.bucket");

        // URL 유효 시간이 만료되면 성공 결과 API를 다시 조회하여 새 URL 발급
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
            .signatureDuration(IMAGE_URL_VALIDITY)
            .getObjectRequest(request -> request.bucket(bucket).key(imageKey))
            .build();

        return presigner.presignGetObject(presignRequest)
                .url().toExternalForm();
    }
}
