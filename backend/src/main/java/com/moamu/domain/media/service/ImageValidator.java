package com.moamu.domain.media.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.springframework.web.multipart.MultipartFile;
import com.moamu.domain.media.exception.MediaErrorCode;
import com.moamu.global.exception.BusinessException;

// 사진 검증 규칙
final class ImageValidator {
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    // 업로드 이미지의 크기와 형식을 검증하고 SHA-256 해시를 계산
    ValidatedImage validate(MultipartFile image) {
        
        // 1. 빈 파일인지 확인
        if (image == null || image.isEmpty()) {
            throw new BusinessException(MediaErrorCode.IMAGE_REQUIRED);
        }

        // 2. 파일 크기 제한 확인
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(MediaErrorCode.IMAGE_TOO_LARGE);
        }

        byte[] bytes;

        try (InputStream input = image.getInputStream()) {
            bytes = input.readNBytes((int) MAX_FILE_SIZE + 1);
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 읽지 못했습니다.", e);
        }

        if (bytes.length == 0 || bytes.length > MAX_FILE_SIZE) {
            throw new BusinessException(bytes.length == 0
                    ? MediaErrorCode.IMAGE_REQUIRED : MediaErrorCode.IMAGE_TOO_LARGE);
        }

        // 3. 지원하는 형식과 실제 이미지인지 검증
        String extension = getExtension(image.getOriginalFilename());
        String contentType = validateImage(bytes, extension);

        // 4. SHA-256 파일 해시 계산
        String fileHash = calculateHash(bytes);

        return new ValidatedImage(bytes, extension, contentType, fileHash);
    }

    // 파일 확장자 확인
    private String getExtension(String originalFilename) {

        // 1. 파일명 존재 여부 확인
        if (originalFilename == null) {
            throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
        }

        // 2. 마지막 마침표(.)의 위치 확인
        int dotIndex = originalFilename.lastIndexOf('.');

        // 3. 확장자가 없는 파일인지 검증
        if (dotIndex < 0) {
            throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
        }

        // 4. 파일명에서 확장자를 추출하고 지원하는 이미지 형식인지 검증
        String extension = originalFilename.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);

        // 5. 지원하는 확장자만 허용하고 jpg/jpeg는 jpg로 통일        
        return switch (extension) {
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            case "webp" -> "webp";
            default -> throw new BusinessException(MediaErrorCode.UNSUPPORTED_IMAGE_FORMAT);
        };
    }

    // 실제 이미지 형식, 확장자 일치 여부 및 해상도 검증
    private String validateImage(byte[] bytes, String extension) {

        // 1. 바이트 배열을 이미지 입력 스트림으로 변환
        try (var input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes)
        )) {
            if (input == null) {
                throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
            }

            // 1. 이미지 데이터를 해석할 수 있는 Reader 확인
            var readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
            }

            var reader = readers.next();

            try {
                reader.setInput(input);

                String format = reader.getFormatName()
                        .toLowerCase(Locale.ROOT);

                String contentType;

                // 2. 파일 확장자와 실제 이미지 형식 일치 여부 검증
                if ("jpg".equals(extension) && "jpeg".equals(format)) {
                    contentType = "image/jpeg";
                } else if ("png".equals(extension) && "png".equals(format)) {
                    contentType = "image/png";
                } else if ("webp".equals(extension) && "webp".equals(format)) {
                    contentType = "image/webp";
                } else {
                    throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                // 3. 과도한 메모리 사용 방지를 위해 최대 2,000만 픽셀로 제한
                if (width <= 0 || height <= 0
                        || (long) width * height > 20_000_000L) {
                    throw new BusinessException(MediaErrorCode.IMAGE_RESOLUTION_TOO_LARGE);
                }

                // 4. 실제 디코딩을 통해 이미지 데이터 유효성 검증
                BufferedImage decoded = reader.read(0);

                if (decoded == null) {
                    throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
                }

                decoded.flush();
                return contentType;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new BusinessException(MediaErrorCode.INVALID_IMAGE);
        }
    }

    // 동일 파일 확인용 SHA-256 해시 계산
    private String calculateHash(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

    record ValidatedImage(byte[] bytes, String extension, String contentType, String fileHash) {}
}
