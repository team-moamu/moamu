package com.moamu.domain.media.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.awt.image.BufferedImage;
import java.security.MessageDigest;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

// 사용자가 올린 사진을 내 폴더에 저장하고, 필요시 삭제하는 클래스
@Service
// 로컬 개발과 테스트에서 같은 파일 저장 구현을 사용합니다.
@Profile({"local", "test"})
public class LocalStorageService implements StorageService {

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private final Path storageRoot;

    // 저장 경로
    public LocalStorageService(
            @Value("${app.storage.local-root}") String root
    ) {
        this.storageRoot = Path.of(root)
                .toAbsolutePath()
                .normalize();
    }

    // 사진을 저장하고 파일 정보 반환
    @Override
    public StoredFile saveVerification(MultipartFile image) {
        // 1. 빈 파일인지 확인
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("사진을 선택해주세요.");
        }

        // 2. 파일 크기 제한 확인
        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("사진은 10MB 이하여야 합니다.");
        }

        byte[] bytes;

        try (InputStream input = image.getInputStream()) {
            bytes = input.readNBytes((int) MAX_FILE_SIZE + 1);
        } catch (IOException e) {
            throw new UncheckedIOException("사진을 읽지 못했습니다.", e);
        }

        if (bytes.length == 0 || bytes.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("사진 크기가 올바르지 않습니다.");
        }

        // 3. 지원하는 형식과 실제 이미지인지 검증
        String extension = getExtension(image.getOriginalFilename());
        String contentType = validateImage(bytes, extension);

        // 4. SHA-256 파일 해시 계산
        String fileHash = calculateHash(bytes);

        // 5. UUID로 파일명 생성
        String fileName = UUID.randomUUID() + "." + extension;
        String imageKey = "verification/" + fileName;

        // 6. verification 폴더 생성 및 파일 저장
        Path target = storageRoot.resolve(imageKey).normalize();

        try {
            Files.createDirectories(target.getParent());
        } catch (IOException e) {
            throw new UncheckedIOException("저장 폴더를 만들지 못했습니다.", e);
        }

        try {
            Files.write(
                    target,
                    bytes,
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (FileAlreadyExistsException e) {
            // 기존 파일은 삭제하거나 덮어쓰지 않음
            throw new UncheckedIOException("같은 이름의 파일이 존재합니다.", e);
        } catch (IOException e) {
            // 저장 도중 실패해서 남은 파일 정리
            try {
                Files.deleteIfExists(target);
            } catch (IOException cleanupError) {
                e.addSuppressed(cleanupError);
            }

            throw new UncheckedIOException("사진 저장에 실패했습니다.", e);
        }

        // 7. 저장된 파일 정보를 StoredFile로 반환
        return new StoredFile(
                imageKey,
                fileHash,
                contentType,
                (long) bytes.length
        );
    }

    // 파일 확장자 확인
    private String getExtension(String originalFilename) {
        if (originalFilename == null) {
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        int dotIndex = originalFilename.lastIndexOf('.');

        if (dotIndex < 0) {
            throw new IllegalArgumentException("파일 확장자가 없습니다.");
        }

        String extension = originalFilename.substring(dotIndex + 1)
                .toLowerCase(Locale.ROOT);

        return switch (extension) {
            case "jpg", "jpeg" -> "jpg";
            case "png" -> "png";
            case "webp" -> "webp";
            default -> throw new IllegalArgumentException(
                    "JPEG, PNG, WebP 사진만 업로드할 수 있습니다."
            );
        };
    }

    // 실제 이미지 형식과 크기 확인
    private String validateImage(byte[] bytes, String extension) {
        try (var input = ImageIO.createImageInputStream(
                new ByteArrayInputStream(bytes)
        )) {
            if (input == null) {
                throw new IllegalArgumentException("이미지를 읽을 수 없습니다.");
            }

            var readers = ImageIO.getImageReaders(input);

            if (!readers.hasNext()) {
                throw new IllegalArgumentException("지원하지 않는 이미지입니다.");
            }

            var reader = readers.next();

            try {
                reader.setInput(input);

                String format = reader.getFormatName()
                        .toLowerCase(Locale.ROOT);

                String contentType;

                if ("jpg".equals(extension) && "jpeg".equals(format)) {
                    contentType = "image/jpeg";
                } else if ("png".equals(extension) && "png".equals(format)) {
                    contentType = "image/png";
                } else if ("webp".equals(extension) && "webp".equals(format)) {
                    contentType = "image/webp";
                } else {
                    throw new IllegalArgumentException(
                            "확장자와 실제 이미지 형식이 다릅니다."
                    );
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                // 압축 파일은 작아도 이미지 해상도가 과도할 수 있으므로 제한
                if (width <= 0 || height <= 0
                        || (long) width * height > 20_000_000L) {
                    throw new IllegalArgumentException(
                            "사진은 2,000만 화소 이하여야 합니다."
                    );
                }

                BufferedImage decoded = reader.read(0);

                if (decoded == null) {
                    throw new IllegalArgumentException("손상된 이미지입니다.");
                }

                decoded.flush();
                return contentType;
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지를 확인하지 못했습니다.", e);
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

    // DB 저장 실패 시 파일 삭제
    @Override
    public void delete(String imageKey) {
        // 1. imageKey가 허용된 verification 폴더 안의 경로인지 확인
        if (imageKey == null || imageKey.isBlank()) {
            throw new IllegalArgumentException("파일 경로가 없습니다.");
        }

        Path verificationRoot = storageRoot.resolve("verification");
        Path relativePath = Path.of(imageKey);

        if (relativePath.isAbsolute()) {
            throw new IllegalArgumentException("상대 경로만 허용됩니다.");
        }

        Path target = storageRoot.resolve(relativePath).normalize();

        // 2. 파일이 존재하면 삭제
        if (!verificationRoot.equals(target.getParent())) {
            throw new IllegalArgumentException("허용되지 않은 파일 경로입니다.");
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException("사진 삭제에 실패했습니다.", e);
        }
    }
}