package com.moamu.domain.gallery.controller;

import com.moamu.domain.gallery.dto.GalleryResponse;
import com.moamu.domain.gallery.dto.GalleryExhibitionResponse;
import com.moamu.domain.gallery.service.GalleryService;
import com.moamu.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/galleries")
@RequiredArgsConstructor
public class GalleryController {

    private final GalleryService galleryService;

    @PatchMapping("/{challengeId}/exhibition")
    public ApiResponse<GalleryExhibitionResponse> hideExhibition(
        @AuthenticationPrincipal Long userId,
        @PathVariable("challengeId") Long challengeId
    ) {
        return ApiResponse.success(galleryService.hideExhibition(userId, challengeId));
    }

    @GetMapping
    public ApiResponse<List<GalleryResponse>> getGalleries(
        @AuthenticationPrincipal Long userId,
        @RequestParam(value = "sort", defaultValue = "endDate") String sort,
        @RequestParam(value = "direction", defaultValue = "desc") String direction
    ) {
        List<GalleryResponse> responses = galleryService.getGalleries(userId, sort, direction);

        return ApiResponse.success(responses);
    }
}
