package com.moamu.domain.verification;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.moamu.domain.verification.repository.VerificationSuccessRepository;
import com.moamu.domain.verification.dto.VerificationSuccessSnapshot;
import com.moamu.domain.verification.enums.VerificationStatus;
import com.moamu.domain.schedule.enums.Status;
import com.moamu.domain.verification.service.ArtworkImageUrlService;
import com.moamu.domain.verification.service.VerificationSuccessService;
import com.moamu.domain.verification.dto.VerificationSuccessResponse.Check;
import com.moamu.domain.verification.enums.CheckStatus;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class VerificationSuccessServiceTests {
    private VerificationSuccessRepository repository;
    private ArtworkImageUrlService images;
    private VerificationSuccessService service;

    @BeforeEach
    void setUp() {
        repository = mock(VerificationSuccessRepository.class);
        images = mock(ArtworkImageUrlService.class);
        service = new VerificationSuccessService(repository, images);
    }

    private VerificationSuccessSnapshot snapshot(String status, String scheduleStatus) {
        return new VerificationSuccessSnapshot(101, VerificationStatus.valueOf(status), 10, "Read", 30, 2,
                Status.valueOf(scheduleStatus), 1,
                3, 3, 20, "Painting", "artworks/test.png");
    }

    @ParameterizedTest
    @ValueSource(ints = {7, 14, 30})
    void returnsAssignedPieceAndCurrentProgressWithScheduleCount(int total) {
        when(repository.findOwned(1, 101)).thenReturn(Optional.of(snapshot("SUCCESS", "SUCCESS")));
        when(repository.findRevealed(10, Status.SUCCESS)).thenReturn(List.of(0, 1));
        when(repository.countPieces(10)).thenReturn((long) total);
        var check = new Check(50, "C1", "Show book", CheckStatus.MET, "Book found");
        when(repository.findChecks(101)).thenReturn(List.of(check));
        when(images.resolve("artworks/test.png")).thenReturn("https://example.com/signed");
        var result = service.getSuccess(1L, 101);
        assertThat(result.openedPieceIndex()).isEqualTo(1);
        assertThat(result.revealedPieceIndices()).containsExactly(0, 1);
        assertThat(result.earnedPieceCount()).isEqualTo(2);
        assertThat(result.totalPieceCount()).isEqualTo(total);
        assertThat(result.checks()).containsExactly(check);
        assertThat(result.artwork().imageUrl()).isEqualTo("https://example.com/signed");
        assertThat(service.getSuccess(1L, 101)).isEqualTo(result);
    }

    @Test
    void verificationSuccessIsTheOnlyEntryCondition() {
        when(repository.findOwned(1, 101)).thenReturn(Optional.of(snapshot("SUCCESS", "PROGRESSING")));
        when(repository.findRevealed(10, Status.SUCCESS)).thenReturn(List.of());
        when(repository.countPieces(10)).thenReturn(7L);
        when(repository.findChecks(101)).thenReturn(List.of());
        var result = service.getSuccess(1L, 101);
        assertThat(result.status()).isEqualTo("SUCCESS");
        assertThat(result.revealedPieceIndices()).isEmpty();
        assertThat(result.artwork().imageUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"SUBMITTED", "ANALYZING", "FAILED", "REVIEW_REQUIRED", "TECH_ERROR"})
    void rejectsUnsuccessfulVerification(String status) {
        when(repository.findOwned(1, 101)).thenReturn(Optional.of(snapshot(status, "OPEN")));
        assertThatThrownBy(() -> service.getSuccess(1L, 101)).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.INVALID_REQUEST));
        verifyNoInteractions(images);
        verify(repository, never()).findRevealed(anyLong(), any());
    }

    @Test
    void rejectsMissingOrOtherOwnersVerification() {
        when(repository.findOwned(2, 101)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getSuccess(2L, 101)).isInstanceOfSatisfying(BusinessException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    @Test
    void rejectsInvalidInputBeforeQuery() {
        assertThatThrownBy(() -> service.getSuccess(null, 101)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getSuccess(0L, 101)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.getSuccess(1L, 0)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(repository, images);
    }
}
