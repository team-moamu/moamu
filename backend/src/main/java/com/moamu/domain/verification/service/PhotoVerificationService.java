package com.moamu.domain.verification.service;

import com.moamu.domain.verification.client.PhotoVerificationClient;
import com.moamu.domain.verification.dto.PhotoVerificationRequest;
import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import com.moamu.domain.verification.enums.CheckStatus;
import com.moamu.domain.verification.enums.VerificationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// AI 조건별 판정을 종합해 최종 인증 결과 결정
@Service
@RequiredArgsConstructor
public class PhotoVerificationService {

    private final PhotoVerificationClient client;

    // AI 서버의 검증된 응답으로 최종 인증 결과를 결정하고 반환
    public Result verify(PhotoVerificationRequest request) {
        PhotoVerificationResponse response = client.verify(request);

        VerificationResult status = VerificationResult.SUCCESS;
        for (var check : response.checks()) {
            if (check.status() == CheckStatus.NOT_MET) {
                status = VerificationResult.FAILED;
                break;
            }
            
            if (check.status() == CheckStatus.UNCERTAIN) {
                status = VerificationResult.REVIEW_REQUIRED;
            }
        }
        return new Result(status, response);
    }

    // 최종 인증 결과와 검증된 AI 응답을 함께 담는 백엔드 내부 반환값
    public record Result(VerificationResult status, PhotoVerificationResponse response) {
    }
}
