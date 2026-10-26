package com.moamu.domain.media.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.moamu.domain.media.dto.MediaResponse;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.media.service.StorageService.StoredFile;
import com.moamu.domain.media.entity.Media;

// 사진 업로드 흐름 관리
// 파일 정보 DB 저장
@Service 
public class MediaService {
    // 실제 사진 파일 저장, 삭제 담당
    private final StorageService storageService;

    // media 테이블에 파일 정보 저장 담당
    private final MediaRepository mediaRepository;

    // DB 저장부터 커밋까지 하나의 트랜잭션으로 실행
    private final TransactionTemplate transactionTemplate;

    // Spring이 필요한 객체를 생성자로 주입
    public MediaService(
            StorageService storageService,
            MediaRepository mediaRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.storageService = storageService;
        this.mediaRepository = mediaRepository;
        this.transactionTemplate =
                new TransactionTemplate(transactionManager);
    }

    // 인증용 사진 업로드
    // userId는 요청 본문이 아니라 인증된 사용자 정보에서 전달받아야 함
    public MediaResponse uploadVerification(
            Long userId,
            MultipartFile image
    ) {
        // 1. 사용자 ID 기본 검증
        // 실제 로그인 여부 확인은 인증 처리에서 담당
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "올바른 사용자 정보가 필요합니다."
            );
        }

        // 2. 실제 사진을 로컬 폴더에 저장
        // 파일 검증/해시 계산/UUID 파일명 생성도 저장소에서 수행
        // 저장에 실패하면 예외가 발생하므로 DB 저장은 진행하지 않음
        StoredFile storedFile =
                storageService.saveVerification(image);

        Media savedMedia;

        try {
            // 3. 파일 정보를 DB에 저장
            // execute()는 트랜잭션 커밋까지 완료된 후 반환
            savedMedia = transactionTemplate.execute(status -> {

                // 저장할 Media 엔티티 생성
                // status=SUCCESS, purpose=VERIFICATION은 reateVerification() 내부에서 설정됨
                Media media = Media.createVerification(
                        storedFile.imageKey(),
                        storedFile.fileHash(),
                        storedFile.contentType(),
                        storedFile.fileSize(),
                        userId
                );

                // INSERT 실행 및 생성된 mediaId 확보
                return mediaRepository.saveAndFlush(media);
            });

        } catch (RuntimeException dbException) {
            // 4. DB 저장 또는 커밋 실패 시 실제 파일도 정리
            // DB 롤백은 로컬 파일까지 삭제해주지 않으므로 직접 처리
            try {
                storageService.delete(storedFile.imageKey());
            } catch (RuntimeException cleanupException) {
                // 파일 삭제까지 실패해도 최초 DB 오류를 보존
                dbException.addSuppressed(cleanupException);
            }

            throw dbException;
        }

        // 5. 저장 결과를 응답 DTO로 변환
        MediaResponse response = new MediaResponse();
        response.setMediaId(savedMedia.getMediaId());
        response.setStatus(savedMedia.getStatus());

        // 여기서는 인증 시도를 생성하지 않으므로
        // verificationId는 설정하지 않음
        return response;
    }
}
