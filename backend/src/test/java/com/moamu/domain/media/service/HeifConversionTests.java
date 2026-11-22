package com.moamu.domain.media.service;

import com.moamu.global.exception.BusinessException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HeifConversionTests {
    @Test
    void keepsOriginalBytesAndPassesDecodedPixelsWithoutIntermediateJpeg() throws Exception {
        byte[] original = {11, 22, 33};
        var decoded = spy(new java.awt.image.BufferedImage(20, 10,
            java.awt.image.BufferedImage.TYPE_INT_ARGB));
        try (var converters = mockConstruction(HeifConverter.class,
                (converter, context) -> when(converter.read(any(byte[].class))).thenReturn(decoded))) {
            try (var validated = new ImageValidator().validate(
                    new MockMultipartFile("image", "photo.heic", "image/heic", original))) {
                assertArrayEquals(original, validated.bytes());
                assertSame(decoded, validated.decodedImage());
                assertEquals("jpg", validated.extension());
                assertEquals("image/jpeg", validated.contentType());
                assertEquals(java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256").digest(original)), validated.fileHash());
                verify(converters.constructed().getFirst()).read(original);
            }
            verify(decoded).flush();
        }
    }

    @Test
    void resizesDecodedHeifPixelsAndUploadsFinalJpeg() throws Exception {
        byte[] original = {11, 22, 33};
        // 실제 HEIC 파일 대신 디코더가 반환할 픽셀을 대역으로 제공해 후속 흐름을 검증합니다.
        var decoded = spy(new java.awt.image.BufferedImage(3200, 1600,
            java.awt.image.BufferedImage.TYPE_INT_ARGB));
        var client = mock(S3Client.class);
        try (var converters = mockConstruction(HeifConverter.class,
                (converter, context) -> when(converter.read(any(byte[].class))).thenReturn(decoded))) {
            var storage = new S3StorageService(client, "test", "verifications", new ImagePreprocessor(1.0f));
            var saved = storage.saveVerification(new MockMultipartFile("image", "photo.heif", "image/heif", original));
            var request = org.mockito.ArgumentCaptor.forClass(
                software.amazon.awssdk.services.s3.model.PutObjectRequest.class);
            var body = org.mockito.ArgumentCaptor.forClass(software.amazon.awssdk.core.sync.RequestBody.class);
            verify(client).putObject(request.capture(), body.capture());
            assertTrue(saved.imageKey().endsWith(".jpg"));
            assertEquals("image/jpeg", request.getValue().contentType());
            assertEquals("image/jpeg", saved.contentType());
            assertEquals(java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(original)), saved.fileHash());
            try (var input = body.getValue().contentStreamProvider().newStream()) {
                byte[] bytes = input.readAllBytes();
                assertEquals(bytes.length, saved.fileSize().longValue());
                assertEquals(0xff, bytes[0] & 255);
                assertEquals(0xd8, bytes[1] & 255);
                var image = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                assertEquals(1600, image.getWidth());
                assertEquals(800, image.getHeight());
                assertFalse(image.getColorModel().hasAlpha());
                assertEquals(java.awt.Color.WHITE.getRGB(), image.getRGB(100, 100));
                image.flush();
            }
            verify(converters.constructed().getFirst()).read(original);
            verify(decoded).flush();
        }
    }

    @Test
    void rejectsMalformedHeifBeforeS3() {
        var client = mock(S3Client.class);
        var storage = new S3StorageService(client, "test", "verifications", new ImagePreprocessor(1.0f));
        assertThrows(BusinessException.class, () -> storage.saveVerification(
            new MockMultipartFile("image", "photo.heif", "image/heif", new byte[]{1, 2, 3})));
        verifyNoInteractions(client);
    }

    @Test
    void rejectsPngDisguisedAsHeic() throws Exception {
        var bytes = new java.io.ByteArrayOutputStream();
        ImageIO.write(new java.awt.image.BufferedImage(2, 2,
            java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        var error = assertThrows(BusinessException.class, () -> new ImageValidator().validate(
            new MockMultipartFile("image", "photo.heic", "image/heic", bytes.toByteArray())));
        assertEquals("INVALID_IMAGE", error.getErrorCode().getCode());
    }

    @Test
    void rejectsOversizedOriginalBeforeConversion() {
        var error = assertThrows(BusinessException.class, () -> new ImageValidator().validate(
            new MockMultipartFile("image", "photo.heic", "image/heic", new byte[10 * 1024 * 1024 + 1])));
        assertEquals("IMAGE_TOO_LARGE", error.getErrorCode().getCode());
    }

}
