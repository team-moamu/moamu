package com.moamu.domain.media.controller;

import com.moamu.domain.media.dto.MediaResponse;
import com.moamu.domain.media.enums.MediaStatus;
import com.moamu.domain.media.service.MediaService;
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
    private MediaService mediaService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mediaService = mock(MediaService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MediaController(mediaService)).build();
    }

    @Test
    void authenticatedUploadReturnsCreatedMedia() throws Exception {
        MediaResponse response = new MediaResponse();
        response.setMediaId(10L);
        response.setStatus(MediaStatus.SUCCESS);
        when(mediaService.uploadVerification(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/media/verification")
                        .file(new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1}))
                        .requestAttr("userId", 1L))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("success").value(true))
                .andExpect(jsonPath("data.mediaId").value(10))
                .andExpect(jsonPath("data.status").value("SUCCESS"));
        verify(mediaService).uploadVerification(eq(1L), any());
    }

    @Test
    void clientSuppliedUserIdDoesNotAuthorizeUpload() throws Exception {
        mockMvc.perform(multipart("/api/media/verification")
                        .param("userId", "1").header("userId", "1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("error.code").value("AUTHENTICATION_REQUIRED"));
        verifyNoInteractions(mediaService);
    }

    @Test
    void invalidServerUserIdDoesNotReachStorage() throws Exception {
        mockMvc.perform(multipart("/api/media/verification").requestAttr("userId", 0L))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(mediaService);
    }
}
