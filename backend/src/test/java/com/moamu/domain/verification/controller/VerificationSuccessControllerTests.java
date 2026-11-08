package com.moamu.domain.verification.controller;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.moamu.domain.verification.dto.VerificationSuccessResponse;
import com.moamu.domain.verification.service.VerificationSuccessService;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class VerificationSuccessControllerTests {
    @Test
    void returnsSuccessResultInApiEnvelope() throws Exception {
        var service = mock(VerificationSuccessService.class);
        var response = new VerificationSuccessResponse(101, "SUCCESS", 10, "Read", 30, 2, "SUCCESS",
                new VerificationSuccessResponse.Artwork(20, "Painting", null), 3, 3, 1,
                List.of(0, 1), 2, 7, List.of());
        when(service.getSuccess(1L, 101)).thenReturn(response);
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(1L, null));
        try {
            MockMvcBuilders.standaloneSetup(new VerificationSuccessController(service))
                    .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                    .build()
                    .perform(get("/api/verification/101/result"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.openedPieceIndex").value(1))
                    .andExpect(jsonPath("$.data.totalPieceCount").value(7));
            verify(service).getSuccess(1L, 101);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
