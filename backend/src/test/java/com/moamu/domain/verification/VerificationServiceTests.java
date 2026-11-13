package com.moamu.domain.verification;

import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.domain.verification.dto.ChallengeVerificationResponseDto;
import com.moamu.domain.verification.service.VerificationService;
import com.moamu.global.exception.BusinessException;
import com.moamu.global.exception.CommonErrorCode;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTests {

    @Mock private ScheduleRepository scheduleRepository;
    @InjectMocks private VerificationService verificationService;

    @Test
    void combinesConditionsAndPreservesScheduleFieldsWithOneRepositoryCall() {
        when(scheduleRepository.findByChallengeIdAndScheduleId(10L, 20L)).thenReturn(List.of(
                row("Show the book"), row("Show the date")));

        ChallengeVerificationResponseDto response = verificationService.getVerification(10L, 20L);

        assertThat(response).isEqualTo(new ChallengeVerificationResponseDto(
                10L, "Reading", 20L, 3, 7, 2, List.of("Show the book", "Show the date")));
        verify(scheduleRepository).findByChallengeIdAndScheduleId(10L, 20L);
        verifyNoMoreInteractions(scheduleRepository);
    }

    @Test
    void returnsEmptyConditionsForLeftJoinWithoutCondition() {
        when(scheduleRepository.findByChallengeIdAndScheduleId(10L, 20L))
                .thenReturn(List.of(row(null)));

        assertThat(verificationService.getVerification(10L, 20L).description()).isEmpty();
    }

    @Test
    void missingOrMismatchedScheduleThrowsCommonNotFound() {
        when(scheduleRepository.findByChallengeIdAndScheduleId(10L, 99L)).thenReturn(List.of());

        assertThatThrownBy(() -> verificationService.getVerification(10L, 99L))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND));
        verify(scheduleRepository).findByChallengeIdAndScheduleId(10L, 99L);
        verifyNoMoreInteractions(scheduleRepository);
    }

    private ChallengeVerificationResponseDto row(String description) {
        return new ChallengeVerificationResponseDto(10L, "Reading", 20L, 3, 7, 2, description);
    }
}
