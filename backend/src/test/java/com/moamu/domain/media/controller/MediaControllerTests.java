package com.moamu.domain.media.controller;

import com.moamu.domain.media.dto.MediaResponse;
import com.moamu.domain.media.service.MediaVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MediaControllerTests {
    private MediaVerificationService mediaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mediaService = mock(MediaVerificationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MediaController(mediaService)).build();
    }

    @Test
    void authenticatedUploadReturnsSavedVerificationResult() throws Exception {
        var response = new MediaResponse(20L, 10L, 30L, "SUCCESS", java.util.List.of());
        when(mediaService.submit(eq(1L), eq(50L), any(), eq("request-1"))).thenReturn(response);

        mockMvc.perform(multipart("/api/media")
                        .file(new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1}))
                        .requestAttr("userId", 1L).param("scheduleId", "50").header("Idempotency-Key", "request-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("success").value(true))
                .andExpect(jsonPath("data.mediaId").value(10))
                .andExpect(jsonPath("data.verificationId").value(20))
                .andExpect(jsonPath("data.status").value("SUCCESS"));
        verify(mediaService).submit(eq(1L), eq(50L), any(), eq("request-1"));
    }

    @Test
    void clientSuppliedUserIdDoesNotAuthorizeUpload() throws Exception {
        mockMvc.perform(multipart("/api/media")
                        .param("userId", "1").header("userId", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("error.code").value("AUTHENTICATION_REQUIRED"));
        verifyNoInteractions(mediaService);
    }

    @Test
    void invalidServerUserIdDoesNotReachStorage() throws Exception {
        mockMvc.perform(multipart("/api/media").requestAttr("userId", 0L))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(mediaService);
    }
}
