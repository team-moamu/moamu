package com.moamu.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Profile ({"local", "prod"})
public class S3Config {

    // 3Presigner: 성공 화면에서 작품 이미지를 조회할 15분 유효한 서명 URL을 생성
    @Bean(destroyMethod = "close")
    public S3Presigner s3Presigner(
            @Value("${app.storage.s3.region}") String region) {
        return S3Presigner.builder()
                .region(Region.of(region)).build();
    }

    // 파일 업로드·삭제 등 S3 작업
    @Bean(destroyMethod = "close")
    public S3Client s3Client (
        @Value("${app.storage.s3.region}") String region
    ) {
        return S3Client.builder().region(Region.of(region)).build();
    }
    
}
