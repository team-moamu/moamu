package com.moamu.domain.media.controller;

import com.moamu.domain.media.dto.MediaResponse;
import com.moamu.domain.media.service.MediaVerificationService;
import com.moamu.domain.user.entity.User;
import com.moamu.domain.user.enums.UserStatus;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.config.SecurityConfig;
import com.moamu.global.config.JwtProperties;
import com.moamu.global.security.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// 테스트 전용 서명 키로 발급한 JWT를 실제 보안 필터 체인에 전달합니다.
@WebMvcTest(controllers = MediaController.class, properties = {
        "jwt.secret=bW9hbXUtdGVzdC1vbmx5LWtleS0zMi1ieXRlcy1sb25nIQ==",
        "jwt.access-token-expiration=1800000"
})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtProperties.class})
class MediaControllerTests {
    @Autowired MockMvc mockMvc;
    @Autowired JwtTokenProvider tokens;
    @MockitoBean MediaVerificationService mediaService;
    @MockitoBean UserRepository users;

    @Test
    void bearerTokenUserReachesSubmissionInsteadOfSuppliedUserId() throws Exception {
        var user = mock(User.class);
        when(user.getStatus()).thenReturn(UserStatus.ACTIVE);
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(mediaService.submit(eq(1L), eq(50L), any(), eq("request-1")))
                .thenReturn(new MediaResponse(20L, 10L, 30L, "SUCCESS", List.of()));

        mockMvc.perform(multipart("/api/media")
                        .file(new MockMultipartFile("image", "guitar.jpg", "image/jpeg", new byte[]{1}))
                        .header("Authorization", "Bearer " + tokens.createAccessToken(1L))
                        .param("scheduleId", "50").header("Idempotency-Key", "request-1")
                        .param("userId", "999").requestAttr("userId", 999L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("success").value(true))
                .andExpect(jsonPath("data.mediaId").value(10))
                .andExpect(jsonPath("data.verificationId").value(20))
                .andExpect(jsonPath("data.status").value("SUCCESS"));
        verify(mediaService).submit(eq(1L), eq(50L), any(), eq("request-1"));
    }

    @Test
    void missingTokenCannotBeReplacedByUserId() throws Exception {
        mockMvc.perform(multipart("/api/media").param("userId", "1")
                        .header("userId", "1").requestAttr("userId", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("error.code").value("AUTHENTICATION_REQUIRED"));
        verifyNoInteractions(mediaService);
    }

    @Test
    void invalidTokenDoesNotReachUpload() throws Exception {
        mockMvc.perform(multipart("/api/media").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("error.code").value("INVALID_TOKEN"));
        verifyNoInteractions(mediaService);
    }

    @Test
    void expiredTokenDoesNotReachUpload() throws Exception {
        var key = io.jsonwebtoken.security.Keys.hmacShaKeyFor(io.jsonwebtoken.io.Decoders.BASE64.decode(
                "bW9hbXUtdGVzdC1vbmx5LWtleS0zMi1ieXRlcy1sb25nIQ=="));
        var expired = io.jsonwebtoken.Jwts.builder().subject("1")
                .expiration(new java.util.Date(0)).signWith(key).compact();
        mockMvc.perform(multipart("/api/media").header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("error.code").value("INVALID_TOKEN"));
        verifyNoInteractions(mediaService);
    }

    @Test
    void tokenForMissingMemberDoesNotReachUpload() throws Exception {
        when(users.findById(1L)).thenReturn(Optional.empty());
        mockMvc.perform(multipart("/api/media").header("Authorization", "Bearer " + tokens.createAccessToken(1L)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(mediaService);
    }
}
