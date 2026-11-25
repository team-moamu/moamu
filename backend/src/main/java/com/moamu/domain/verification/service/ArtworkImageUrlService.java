package com.moamu.domain.verification.service;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class ArtworkImageUrlService {

    private static final Logger logger = LoggerFactory.getLogger(ArtworkImageUrlService.class);
    private static final Duration IMAGE_URL_VALIDITY = Duration.ofMinutes(15);
    private static final int PREVIEW_SAMPLE_SIZE = 12;
    private static final int PREVIEW_SIZE = 96;

    private final ObjectProvider<S3Presigner> presigners;
    private final ObjectProvider<S3Client> s3Clients;
    private final Environment environment;

    public ArtworkImageUrlService(ObjectProvider<S3Presigner> presigners, ObjectProvider<S3Client> s3Clients,
            Environment environment) {
        this.presigners = presigners;
        this.s3Clients = s3Clients;
        this.environment = environment;
    }

    public String resolveBlurredPreview(String imageKey) {
        S3Client s3 = s3Clients.getIfAvailable();
        if (s3 == null || imageKey == null || imageKey.isBlank()) {
            return null;
        }
        String bucket = environment.getRequiredProperty("app.storage.s3.bucket");
        try (var object = s3.getObject(request -> request.bucket(bucket).key(imageKey)
                .overrideConfiguration(config -> config.apiCallTimeout(Duration.ofSeconds(5))))) {
            BufferedImage original = ImageIO.read(object);
            if (original == null) {
                return null;
            }
            BufferedImage sample = resize(original, PREVIEW_SAMPLE_SIZE,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
            BufferedImage preview = resize(sample, PREVIEW_SIZE, RenderingHints.VALUE_INTERPOLATION_BICUBIC, false);
            var output = new ByteArrayOutputStream();
            ImageIO.write(preview, "png", output);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException | SdkException exception) {
            logger.warn("Could not load artwork preview: {}", exception.getClass().getSimpleName());
            return null;
        }
    }

    private BufferedImage resize(BufferedImage source, int size, Object interpolation, boolean downsample) {
        double scale = (double) size / Math.max(source.getWidth(), source.getHeight());
        int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
        int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
        BufferedImage target = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpolation);
            Image image = downsample ? source.getScaledInstance(width, height, Image.SCALE_AREA_AVERAGING) : source;
            graphics.drawImage(image, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return target;
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
