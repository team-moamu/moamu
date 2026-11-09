package com.moamu.domain.media.dto;

import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// 사진 저장과 AI 인증 결과를 담는 응답 DTO
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    // 사진 저장 단계에서는 mediaId만 설정
    // 인증 완료 후 나머지 값 채우기
    private Long verificationId;
    private Long mediaId;
    private Long aiTaskId;
    private String status;
    private List<PhotoVerificationResponse.Check> checks;
}
