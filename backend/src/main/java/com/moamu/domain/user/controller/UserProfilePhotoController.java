package com.moamu.domain.user.controller;

import com.moamu.domain.user.dto.UserProfilePhotoResponse;
import com.moamu.domain.user.service.UserProfilePhotoService;
import com.moamu.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/users/me/profile-photo")
@RequiredArgsConstructor
public class UserProfilePhotoController {

  private final UserProfilePhotoService userProfilePhotoService;

  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ApiResponse<UserProfilePhotoResponse> uploadProfilePhoto(
      @AuthenticationPrincipal Long userId,
      @RequestParam("image") MultipartFile image) {
    Long profileMediaId = userProfilePhotoService.uploadProfilePhoto(userId, image);

    return ApiResponse.success(new UserProfilePhotoResponse(profileMediaId));
  }

  @DeleteMapping
  public ResponseEntity<Void> resetProfilePhoto(
      @AuthenticationPrincipal Long userId) {
    userProfilePhotoService.resetProfilePhoto(userId);
    return ResponseEntity.noContent().build();
  }
}