package com.moamu.global.exception;

import java.util.Map;

import com.moamu.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공통 응답 형식과 예외 처리 검증함.
 * MockMvc 사용함. DB 연결 및 Security 필터 검증은 제외함.
 */
class GlobalExceptionHandlerTests {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // 테스트마다 Controller와 예외 처리기 연결함.
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // 성공 응답에 success, data만 포함되는지 확인함.
    @Test
    void successContainsOnlySuccessAndData() throws Exception {
        mockMvc.perform(get("/test/success"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.challengeId").value(1))
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    // 도메인 오류의 상태·코드·메시지 확인함. data는 제외함.
    @Test
    void businessExceptionUsesDomainStatusAndCode() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("TEST_CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("테스트 충돌"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    // 필수값 누락 시 400, INVALID_REQUEST 반환 확인함.
    @Test
    void invalidBodyReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/test/request").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    // 잘못된 JSON 요청의 400 응답 확인함.
    @Test
    void malformedJsonReturnsBadRequest() throws Exception {
        mockMvc.perform(post("/test/request").contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    // 파라미터 타입 불일치 시 400 응답 확인함.
    @Test
    void invalidParameterReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/test/parameter").param("id", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    // 미지원 메서드의 405 상태와 Allow 헤더 유지 확인함.
    @Test
    void unsupportedMethodPreservesStatusAndAllowHeader() throws Exception {
        mockMvc.perform(post("/test/success"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    // 명시한 404 상태가 유지되는지 확인함.
    @Test
    void responseStatusExceptionPreservesNotFoundStatus() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("INVALID_REQUEST"));
    }

    // 서버 오류 시 500 반환 및 내부 메시지 미노출 확인함.
    @Test
    void unexpectedExceptionDoesNotExposeInternalMessage() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.message").value("서버 오류가 발생했습니다."))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("internal-detail"))));
    }

    // 응답·예외 재현용 Controller임. 운영 JAR에는 포함 안 됨.
    @RestController
    static class TestController {

        @GetMapping("/test/success")
        public ApiResponse<Map<String, Integer>> success() {
            return ApiResponse.success(Map.of("challengeId", 1));
        }

        @GetMapping("/test/business")
        public void business() {
            throw new BusinessException(new ErrorCode() {
                public HttpStatus getHttpStatus() { return HttpStatus.CONFLICT; }
                public String getCode() { return "TEST_CONFLICT"; }
                public String getMessage() { return "테스트 충돌"; }
            });
        }

        @PostMapping("/test/request")
        public ApiResponse<Request> request(@Valid @RequestBody Request request) {
            return ApiResponse.success(request);
        }

        @GetMapping("/test/parameter")
        public ApiResponse<Integer> parameter(@RequestParam("id") Integer id) {
            return ApiResponse.success(id);
        }

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        @GetMapping("/test/unexpected")
        public void unexpected() {
            throw new IllegalStateException("internal-detail");
        }
    }

    // 검증용 DTO임. name은 null·빈 문자열·공백 허용 안 함.
    record Request(@NotBlank String name) {
    }
}
