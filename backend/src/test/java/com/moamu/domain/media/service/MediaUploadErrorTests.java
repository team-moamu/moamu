package com.moamu.domain.media.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.moamu.global.exception.GlobalExceptionHandler;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MediaUploadErrorTests {
    // 실제 S3 연결 없이 검증과 S3 장애의 HTTP 응답을 확인합니다.
    private final software.amazon.awssdk.services.s3.S3Client client =
            org.mockito.Mockito.mock(software.amazon.awssdk.services.s3.S3Client.class);

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new UploadController(new S3StorageService(client, "test-bucket", "verifications/")))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // 실제 서비스의 검증 예외가 HTTP 상태와 오류 코드로 변환되는지 확인합니다.
    @Test
    void emptyAndUnsupportedFilesReturn400() throws Exception {
        mvc().perform(multipart("/test/upload").file(new MockMultipartFile("image", new byte[0])))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("IMAGE_REQUIRED"));
        mvc().perform(multipart("/test/upload").file(new MockMultipartFile("image", "file.txt", "text/plain", new byte[]{1})))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("UNSUPPORTED_IMAGE_FORMAT"));
        mvc().perform(multipart("/test/upload").file(new MockMultipartFile("image", "file.png", "image/png", new byte[]{1})))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_IMAGE"));
    }

    @Test
    void oversizedFileReturns413() throws Exception {
        mvc().perform(multipart("/test/upload").file(new MockMultipartFile("image", new byte[10 * 1024 * 1024 + 1])))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.error.code").value("IMAGE_TOO_LARGE"));
    }

    @Test
    void s3FailureStillReturns500() throws Exception {
        // S3 업로드 장애는 사용자 입력 오류가 아닌 서버 오류로 응답합니다.
        org.mockito.Mockito.when(client.putObject(
                org.mockito.ArgumentMatchers.any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class),
                org.mockito.ArgumentMatchers.any(software.amazon.awssdk.core.sync.RequestBody.class)))
                .thenThrow(software.amazon.awssdk.services.s3.model.S3Exception.builder()
                        .message("upload failed").statusCode(503).build());
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        mvc().perform(multipart("/test/upload").file(new MockMultipartFile("image", "file.png", "image/png", bytes.toByteArray())))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
    }

    // 테스트 전용 HTTP 진입점입니다. 운영 업로드 API를 추가하는 것은 아닙니다.
    @RestController
    static class UploadController {
        private final StorageService storage;
        UploadController(StorageService storage) { this.storage = storage; }
        @PostMapping("/test/upload")
        public void upload(@RequestParam("image") MultipartFile image) { storage.saveVerification(image); }
    }
}
