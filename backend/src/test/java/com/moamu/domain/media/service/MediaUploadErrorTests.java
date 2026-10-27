package com.moamu.domain.media.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
    @TempDir Path root;

    private MockMvc mvc(Path path) {
        return MockMvcBuilders.standaloneSetup(new UploadController(new LocalStorageService(path.toString())))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    // 실제 서비스의 검증 예외가 HTTP 상태와 오류 코드로 변환되는지 확인합니다.
    @Test
    void emptyAndUnsupportedFilesReturn400() throws Exception {
        mvc(root).perform(multipart("/test/upload").file(new MockMultipartFile("image", new byte[0])))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("IMAGE_REQUIRED"));
        mvc(root).perform(multipart("/test/upload").file(new MockMultipartFile("image", "file.txt", "text/plain", new byte[]{1})))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("UNSUPPORTED_IMAGE_FORMAT"));
        mvc(root).perform(multipart("/test/upload").file(new MockMultipartFile("image", "file.png", "image/png", new byte[]{1})))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("INVALID_IMAGE"));
    }

    @Test
    void oversizedFileReturns413() throws Exception {
        mvc(root).perform(multipart("/test/upload").file(new MockMultipartFile("image", new byte[10 * 1024 * 1024 + 1])))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.error.code").value("IMAGE_TOO_LARGE"));
    }

    @Test
    void diskFailureStillReturns500() throws Exception {
        // 저장 루트가 디렉터리가 아닌 파일이면 실제 파일 쓰기 단계에서 실패합니다.
        Path invalidRoot = Files.createFile(root.resolve("not-a-directory"));
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", bytes);
        mvc(invalidRoot).perform(multipart("/test/upload").file(new MockMultipartFile("image", "file.png", "image/png", bytes.toByteArray())))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"));
    }

    // 테스트 전용 HTTP 진입점입니다. 운영 업로드 API를 추가하는 것은 아닙니다.
    @RestController
    static class UploadController {
        private final LocalStorageService storage;
        UploadController(LocalStorageService storage) { this.storage = storage; }
        @PostMapping("/test/upload")
        public void upload(@RequestParam("image") MultipartFile image) { storage.saveVerification(image); }
    }
}
