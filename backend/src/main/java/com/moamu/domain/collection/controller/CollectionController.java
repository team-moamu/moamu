package com.moamu.domain.collection.controller;

import com.moamu.domain.collection.dto.CollectionResponse;
import com.moamu.domain.collection.service.CollectionService;
import com.moamu.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    @GetMapping
    public ApiResponse<List<CollectionResponse>> getCollections(
        @AuthenticationPrincipal Long userId){
        // 사용자의 완료된 도전 목록 조회
        List<CollectionResponse> responses =
            collectionService.getCollections(userId);

        return ApiResponse.success(responses);
    }

}
