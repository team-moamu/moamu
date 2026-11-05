package com.moamu.domain.media.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HexFormat;
import java.security.MessageDigest;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import com.moamu.global.exception.BusinessException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class S3StorageServiceTests {
    private final S3Client client = mock(S3Client.class);
    private final S3StorageService storage = new S3StorageService(client, "test-bucket", "verifications");

    private byte[] png() throws Exception {
        var out = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
        return out.toByteArray();
    }

    @Test
    void uploadsValidatedBytesAndReturnsMetadata() throws Exception {
        byte[] bytes = png();
        var file = new MockMultipartFile("image", "photo.png", "image/png", bytes);
        var saved = storage.saveVerification(file);
        var request = ArgumentCaptor.forClass(PutObjectRequest.class);
        var body = ArgumentCaptor.forClass(RequestBody.class);
        verify(client).putObject(request.capture(), body.capture());
        assertEquals("test-bucket", request.getValue().bucket());
        assertEquals(saved.imageKey(), request.getValue().key());
        assertTrue(saved.imageKey().matches("verifications/[0-9a-f-]{36}\\.png"));
        assertEquals("image/png", request.getValue().contentType());
        assertEquals("image/png", saved.contentType());
        assertEquals((long) bytes.length, saved.fileSize());
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)), saved.fileHash());
        try (var input = body.getValue().contentStreamProvider().newStream()) {
            assertArrayEquals(bytes, input.readAllBytes());
        }
    }

    @Test
    void invalidImagesNeverReachS3() {
        assertThrows(BusinessException.class, () -> storage.saveVerification(
                new MockMultipartFile("image", "fake.png", "image/png", new byte[]{1})));
        verifyNoInteractions(client);
    }

    @Test
    void uploadFailurePropagatesToPreventSuccessMetadata() throws Exception {
        var error = S3Exception.builder().message("upload failed").statusCode(503).build();
        when(client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenThrow(error);
        var file = new MockMultipartFile("image", "photo.png", "image/png", png());
        assertSame(error, assertThrows(S3Exception.class, () -> storage.saveVerification(file)));
    }

    @Test
    void deletesOnlyConfiguredVerificationKey() {
        storage.delete("verifications/photo.png");
        var request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(client).deleteObject(request.capture());
        assertEquals("test-bucket", request.getValue().bucket());
        assertEquals("verifications/photo.png", request.getValue().key());
    }

    @Test
    void rejectsKeysOutsideScope() {
        for (String key : new String[]{"profiles/photo.png", "verifications-other/photo.png",
                "verifications/", "verifications/../photo.png"}) {
            assertThrows(IllegalArgumentException.class, () -> storage.delete(key));
        }
        assertThrows(IllegalArgumentException.class, () -> storage.delete(null));
        verifyNoInteractions(client);
    }

    @Test
    void deletionFailureIsNotHidden() {
        var error = S3Exception.builder().message("delete failed").statusCode(503).build();
        when(client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(error);
        assertSame(error, assertThrows(S3Exception.class, () -> storage.delete("verifications/photo.png")));
    }
}
