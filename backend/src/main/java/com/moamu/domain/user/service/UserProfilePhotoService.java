package com.moamu.domain.user.service;

import com.moamu.domain.media.entity.Media;
import com.moamu.domain.media.repository.MediaRepository;
import com.moamu.domain.media.service.StorageService;
import com.moamu.domain.user.exception.UserErrorCode;
import com.moamu.domain.user.repository.UserRepository;
import com.moamu.global.exception.BusinessException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfilePhotoService {

  private final StorageService storageService;
  private final MediaRepository mediaRepository;
  private final UserRepository userRepository;
  private final TransactionTemplate transactionTemplate;

  public UserProfilePhotoService(
      StorageService storageService,
      MediaRepository mediaRepository,
      UserRepository userRepository,
      PlatformTransactionManager transactionManager) {
    this.storageService = storageService;
    this.mediaRepository = mediaRepository;
    this.userRepository = userRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public Long uploadProfilePhoto(Long userId, MultipartFile image) {
    var storedFile = storageService.saveProfile(image);

    try {
      return transactionTemplate.execute(status -> {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

        var media = Media.createProfile(
            storedFile.imageKey(),
            storedFile.fileHash(),
            storedFile.contentType(),
            storedFile.fileSize(),
            userId);

        var savedMedia = mediaRepository.saveAndFlush(media);
        user.changeProfilePhoto(savedMedia.getMediaId());

        return savedMedia.getMediaId();
      });
    } catch (RuntimeException dbException) {
      try {
        storageService.deleteProfile(storedFile.imageKey());
      } catch (RuntimeException cleanupException) {
        dbException.addSuppressed(cleanupException);
      }

      throw dbException;
    }
  }

  @Transactional
  public void resetProfilePhoto(Long userId) {
    var user = userRepository.findById(userId)
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    user.changeProfilePhoto(null);
  }
}