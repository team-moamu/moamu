package com.moamu.domain.verification.service;

import com.moamu.domain.schedule.repository.ScheduleRepository;
import com.moamu.domain.verification.dto.PhotoVerificationRequest;
import com.moamu.domain.verification.dto.PhotoVerificationResponse;
import com.moamu.domain.verification.enums.CheckStatus;
import com.moamu.domain.verification.enums.VerificationResult;
import com.moamu.domain.verification.exception.PhotoVerificationTechnicalException;
import com.moamu.domain.verification.repository.VerificationResultRepository;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VerificationResultService {
    private final VerificationResultRepository repository;
    private final Validator validator;
    private final ScheduleRepository schedules;

    public VerificationResultService(VerificationResultRepository repository, Validator validator,
            ScheduleRepository schedules) {
        this.repository = repository;
        this.validator = validator;
        this.schedules = schedules;
    }

    // AI 호출 후 조건별 판정·인증 결과·작업 완료·회차 상태를 같은 트랜잭션에 저장
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public PhotoVerificationService.Result save(
            PhotoVerificationRequest request, PhotoVerificationService.Result result) {
        validate(request, result);
        long taskId = parseId(request.aiTaskId());
        long verificationId = parseId(request.verificationId());
        var response = result.response();
        var conditions = repository.findConditions(verificationId);
        Map<String, VerificationResultRepository.Condition> byCode = new HashMap<>();
        for (var condition : conditions) {
            if (byCode.put(condition.code(), condition) != null) {
                throw technical("저장된 인증 조건 코드가 중복되었습니다.");
            }
        }
        if (conditions.size() != request.conditions().size()) {
            throw technical("요청 조건과 저장된 인증 조건이 다릅니다.");
        }
        for (var condition : request.conditions()) {
            var snapshot = byCode.get(condition.code());
            if (snapshot == null || !snapshot.description().equals(condition.description())) {
                throw technical("요청 조건과 저장된 인증 조건이 다릅니다.");
            }
        }
        var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        if (!repository.completeTask(taskId, verificationId, now)) {
            return repository.findCompleted(taskId, verificationId)
                    .orElseThrow(() -> technical("결과를 저장할 수 없는 AI 작업 상태입니다."));
        }
        if (!repository.completeVerification(verificationId, result.status(), summary(response), response.modelVersion(), now)) {
            throw technical("이미 확정되었거나 존재하지 않는 인증 시도입니다.");
        }
        for (var check : response.checks()) {
            repository.insertCheck(verificationId, byCode.get(check.code()).id(), check, now);
        }
        var scheduleId = repository.findLatestAttemptScheduleId(verificationId)
                .orElseThrow(() -> technical("최신 인증 시도가 아니므로 회차에 반영할 수 없습니다."));
        var schedule = schedules.findById(scheduleId)
                .orElseThrow(() -> technical("인증 회차를 찾을 수 없습니다."));
        schedule.applyVerificationResult(result.status(), summary(response), now);
        // JPA의 @Version 검사 실패 시 앞서 저장한 판정과 작업 완료도 함께 롤백됩니다.
        schedules.flush();
        return result;
    }

    // 내부 메서드가 직접 호출되어도 잘못된 응답을 DB에 기록하지 않도록 다시 검증
    private void validate(PhotoVerificationRequest request, PhotoVerificationService.Result result) {
        if (request == null || !validator.validate(request).isEmpty()
                || result == null || result.response() == null
                || !validator.validate(result.response()).isEmpty()) {
            throw technical("판정 저장에 필요한 값이 올바르지 않습니다.");
        }
        var response = result.response();
        if (!request.aiTaskId().equals(response.aiTaskId())
                || !request.verificationId().equals(response.verificationId())
                || !"SUCCEEDED".equals(response.processingStatus())) {
            throw technical("판정 응답 ID 또는 처리 상태가 올바르지 않습니다.");
        }
        var expected = new java.util.HashSet<String>();
        for (var condition : request.conditions()) {
            if (!expected.add(condition.code())) throw technical("요청 조건 코드가 중복되었습니다.");
        }
        var actual = new java.util.HashSet<String>();
        VerificationResult calculated = VerificationResult.SUCCESS;
        for (var check : response.checks()) {
            if (!actual.add(check.code())) throw technical("응답 조건 코드가 중복되었습니다.");
            if (check.status() == CheckStatus.NOT_MET) calculated = VerificationResult.FAILED;
            else if (check.status() == CheckStatus.UNCERTAIN && calculated != VerificationResult.FAILED)
                calculated = VerificationResult.REVIEW_REQUIRED;
        }
        if (!expected.equals(actual) || calculated != result.status()) {
            throw technical("조건별 판정과 최종 결과가 올바르지 않습니다.");
        }
    }

    private String summary(PhotoVerificationResponse response) {
        for (var check : response.checks()) if (check.status() == CheckStatus.NOT_MET) return check.reason();
        for (var check : response.checks()) if (check.status() == CheckStatus.UNCERTAIN) return check.reason();
        return "모든 인증 조건을 충족했습니다.";
    }

    private long parseId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id > 0) return id;
        } catch (NumberFormatException ignored) {
        }
        throw technical("판정 저장 ID가 올바르지 않습니다.");
    }

    private PhotoVerificationTechnicalException technical(String message) {
        return new PhotoVerificationTechnicalException(message);
    }
}
