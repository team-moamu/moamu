package com.moamu.domain.media.dto;

import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사진 접수 상태 또는 AI 인증 완료 결과를 담는 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    // 사진 저장 단계에서는 mediaId만 설정
    // 접수 시 ID와 SUBMITTED 상태를 반환하고, checks는 완료 후 결과 조회에서 반환
    private Long verificationId;
    private Long mediaId;
    private Long aiTaskId;
    private String status;
    private List<PhotoVerificationResponse.Check> checks;
}
