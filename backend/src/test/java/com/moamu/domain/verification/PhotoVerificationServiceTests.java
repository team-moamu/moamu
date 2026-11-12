package com.moamu.domain.verification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.moamu.domain.verification.client.PhotoVerificationClient;
import com.moamu.domain.verification.dto.PhotoVerificationRequest;
import com.moamu.domain.verification.dto.PhotoVerificationRequest.Condition;
import com.moamu.domain.verification.enums.VerificationResult;
import com.moamu.domain.verification.exception.PhotoVerificationTechnicalException;
import com.moamu.domain.verification.service.PhotoVerificationService;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class PhotoVerificationServiceTests {

    private static final String SERVER_URL = "http://ai-server.test";
    private static final String ENDPOINT = SERVER_URL + "/internal/photo-verifications";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final ValidatorFactory VALIDATORS = Validation.buildDefaultValidatorFactory();
    private static final List<Condition> CONDITIONS = List.of(
            new Condition("C1", "명찰이 사진에 보여야 한다"),
            new Condition("C2", "완성된 결과물이 보여야 한다"));
    private static final PhotoVerificationRequest REQUEST = new PhotoVerificationRequest(
            "9007199254740993", "22", "verifications/photo.jpg", CONDITIONS);
    private static final String SUCCESS_JSON = """
            {
              "ai_task_id": "9007199254740993",
              "verification_id": "22",
              "processing_status": "SUCCEEDED",
              "checks": [
                {"code": "C1", "status": "MET", "reason": "명찰이 확인됩니다."},
                {"code": "C2", "status": "MET", "reason": "결과물이 확인됩니다."}
              ],
              "model_version": "photo-verifier-v1"
            }
            """;

    private MockRestServiceServer server;
    private PhotoVerificationService service;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        service = serviceFor(SERVER_URL);
    }

    @AfterEach
    void verifyRequests() {
        server.verify();
    }

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATORS.close();
    }

    @Test
    void sendsOnlySpecifiedFieldsAndPreservesStringIdsAndResponseDetails() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header("Accept", "application/json"))
                .andExpect(content().json("""
                        {
                          "ai_task_id": "9007199254740993",
                          "verification_id": "22",
                          "image_key": "verifications/photo.jpg",
                          "conditions": [
                            {"code": "C1", "description": "명찰이 사진에 보여야 한다"},
                            {"code": "C2", "description": "완성된 결과물이 보여야 한다"}
                          ]
                        }
                        """, true))
                .andRespond(withSuccess(SUCCESS_JSON, MediaType.APPLICATION_JSON));

        var result = service.verify(REQUEST);

        assertThat(result.status()).isEqualTo(VerificationResult.SUCCESS);
        assertThat(result.response().aiTaskId()).isEqualTo(REQUEST.aiTaskId());
        assertThat(result.response().verificationId()).isEqualTo(REQUEST.verificationId());
        assertThat(result.response().modelVersion()).isEqualTo("photo-verifier-v1");
        assertThat(result.response().checks().getFirst().reason()).isEqualTo("명찰이 확인됩니다.");
    }

    @ParameterizedTest
    @CsvSource({
            "MET,MET,SUCCESS",
            "MET,NOT_MET,FAILED", "NOT_MET,MET,FAILED", "NOT_MET,NOT_MET,FAILED",
            "MET,UNCERTAIN,REVIEW_REQUIRED", "UNCERTAIN,MET,REVIEW_REQUIRED",
            "UNCERTAIN,UNCERTAIN,REVIEW_REQUIRED",
            "UNCERTAIN,NOT_MET,FAILED", "NOT_MET,UNCERTAIN,FAILED"
    })
    void decidesResultOnlyAfterSuccessfulAnalysis(String first, String second, VerificationResult expected)
            throws Exception {
        ObjectNode response = success();
        ((ObjectNode) response.withArray("checks").get(0)).put("status", first);
        ((ObjectNode) response.withArray("checks").get(1)).put("status", second);
        respond(response.toString());

        assertThat(service.verify(REQUEST).status()).isEqualTo(expected);
    }

    @Test
    void matchesConditionsByCodeRegardlessOfOrderAndAcceptsMaximumLengths() throws Exception {
        ObjectNode response = success();
        var checks = response.withArray("checks");
        var first = checks.remove(0);
        checks.add(first);
        ((ObjectNode) checks.get(0)).put("reason", "가".repeat(500));
        response.put("model_version", "v".repeat(50));
        respond(response.toString());

        assertThat(service.verify(REQUEST).status()).isEqualTo(VerificationResult.SUCCESS);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void rejectsInvalidRequestsBeforeCallingAi(String description, PhotoVerificationRequest request) {
        assertThatThrownBy(() -> service.verify(request)).isInstanceOf(PhotoVerificationTechnicalException.class);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                Arguments.of("null 요청", null),
                Arguments.of("작업 ID 누락", new PhotoVerificationRequest(null, "22", "photo.jpg", CONDITIONS)),
                Arguments.of("인증 ID 공백", new PhotoVerificationRequest("1", " ", "photo.jpg", CONDITIONS)),
                Arguments.of("이미지 키 누락", new PhotoVerificationRequest("1", "22", null, CONDITIONS)),
                Arguments.of("이미지 키 공백", new PhotoVerificationRequest("1", "22", " ", CONDITIONS)),
                Arguments.of("조건 목록 누락", new PhotoVerificationRequest("1", "22", "photo.jpg", null)),
                Arguments.of("빈 조건 목록", new PhotoVerificationRequest("1", "22", "photo.jpg", List.of())),
                Arguments.of("null 조건", new PhotoVerificationRequest("1", "22", "photo.jpg", Arrays.asList((Condition) null))),
                Arguments.of("조건 코드 누락", new PhotoVerificationRequest("1", "22", "photo.jpg", List.of(new Condition(null, "조건")))),
                Arguments.of("조건 설명 공백", new PhotoVerificationRequest("1", "22", "photo.jpg", List.of(new Condition("C1", " ")))),
                Arguments.of("조건 코드 중복", new PhotoVerificationRequest("1", "22", "photo.jpg", List.of(CONDITIONS.getFirst(), CONDITIONS.getFirst())))
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidResponses")
    void invalidAiResponsesAreTechnicalErrorsInsteadOfUserResults(String description, String body) {
        respond(body);

        assertThatThrownBy(() -> service.verify(REQUEST)).isInstanceOf(PhotoVerificationTechnicalException.class);
    }

    static Stream<Arguments> invalidResponses() throws Exception {
        return Stream.of(
                Arguments.of("빈 본문", ""), Arguments.of("JSON null", "null"),
                Arguments.of("잘못된 JSON", "{broken}"), Arguments.of("배열 응답", "[]"),
                Arguments.of("추가 JSON 본문", SUCCESS_JSON + "{}"),
                changed("작업 ID 불일치", json -> json.put("ai_task_id", "2")),
                changed("인증 ID 불일치", json -> json.put("verification_id", "23")),
                changed("작업 ID 누락", json -> json.remove("ai_task_id")),
                changed("문자열이 아닌 ID", json -> json.put("verification_id", 22)),
                changed("처리 상태 누락", json -> json.remove("processing_status")),
                changed("잘못된 처리 상태", json -> json.put("processing_status", "ERROR")),
                changed("조건 목록 누락", json -> json.remove("checks")),
                changed("빈 결과 목록", json -> json.putArray("checks")),
                changed("null 결과", json -> json.withArray("checks").addNull()),
                changed("조건 누락", json -> json.withArray("checks").remove(1)),
                changed("조건 추가", json -> json.withArray("checks").addObject()
                        .put("code", "C3").put("status", "MET").put("reason", "사유")),
                changed("조건 중복", json -> ((ObjectNode) json.withArray("checks").get(1)).put("code", "C1")),
                changed("알 수 없는 조건", json -> ((ObjectNode) json.withArray("checks").get(1)).put("code", "C3")),
                changed("결과 코드 누락", json -> ((ObjectNode) json.withArray("checks").get(0)).remove("code")),
                changed("판정값 누락", json -> ((ObjectNode) json.withArray("checks").get(0)).remove("status")),
                changed("잘못된 판정값", json -> ((ObjectNode) json.withArray("checks").get(0)).put("status", "PASSED")),
                changed("숫자 판정값", json -> ((ObjectNode) json.withArray("checks").get(0)).put("status", 0)),
                changed("사유 누락", json -> ((ObjectNode) json.withArray("checks").get(0)).remove("reason")),
                changed("사유 공백", json -> ((ObjectNode) json.withArray("checks").get(0)).put("reason", " ")),
                changed("사유 500자 초과", json -> ((ObjectNode) json.withArray("checks").get(0)).put("reason", "가".repeat(501))),
                changed("모델 버전 누락", json -> json.remove("model_version")),
                changed("모델 버전 공백", json -> json.put("model_version", " ")),
                changed("모델 버전 50자 초과", json -> json.put("model_version", "v".repeat(51))),
                Arguments.of("JSON 필드 중복", SUCCESS_JSON.replace("\"verification_id\": \"22\"", "\"verification_id\": \"23\", \"verification_id\": \"22\""))
        );
    }

    @ParameterizedTest
    @CsvSource({"400,INVALID_REQUEST", "422,UNSUPPORTED_IMAGE", "422,IMAGE_FETCH_FAILED",
            "500,INTERNAL_ERROR", "503,MODEL_UNAVAILABLE"})
    void specifiedAiErrorsAreTechnicalErrors(int status, String code) {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON).body(errorJson(code)));

        assertThatThrownBy(() -> service.verify(REQUEST))
                .isInstanceOf(PhotoVerificationTechnicalException.class)
                .hasMessage("AI 판정 기술 오류: " + code);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidErrorResponses")
    void validatesErrorResponseIdsStatusAndRequiredFields(String description, String body) {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON).body(body));

        assertThatThrownBy(() -> service.verify(REQUEST))
                .isInstanceOf(PhotoVerificationTechnicalException.class)
                .hasMessageNotContaining("AI 판정 기술 오류:");
    }

    static Stream<Arguments> invalidErrorResponses() {
        String error = errorJson("MODEL_UNAVAILABLE");
        return Stream.of(
                Arguments.of("오류 본문 누락", ""),
                Arguments.of("오류 본문 형식 오류", "<html>unavailable</html>"),
                Arguments.of("오류 응답 작업 ID 불일치", error.replace("9007199254740993", "2")),
                Arguments.of("오류 응답 인증 ID 불일치", error.replace("\"22\"", "\"23\"")),
                Arguments.of("오류 처리 상태 오류", error.replace("\"ERROR\"", "\"SUCCEEDED\"")),
                Arguments.of("오류 필드 누락", error.replace("\"error\"", "\"other\"")),
                Arguments.of("오류 메시지 공백", error.replace("모델 사용 불가", " ")),
                Arguments.of("오류 코드와 HTTP 상태 불일치", errorJson("INVALID_REQUEST")),
                Arguments.of("명세에 없는 오류 코드", errorJson("UNKNOWN"))
        );
    }

    @ParameterizedTest
    @ValueSource(ints = {201, 202, 204, 302})
    void onlyHttp200CanBeAnAnalysisSuccess(int status) {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.valueOf(status))
                .contentType(MediaType.APPLICATION_JSON).body(SUCCESS_JSON));

        assertThatThrownBy(() -> service.verify(REQUEST)).isInstanceOf(PhotoVerificationTechnicalException.class);
    }

    @Test
    void timeoutDoesNotReturnFailedAndRetryKeepsOriginalIds() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(jsonPath("$.ai_task_id").value(REQUEST.aiTaskId()))
                .andExpect(jsonPath("$.verification_id").value(REQUEST.verificationId()))
                .andRespond(withException(new SocketTimeoutException("timeout")));
        server.expect(requestTo(ENDPOINT))
                .andExpect(jsonPath("$.ai_task_id").value(REQUEST.aiTaskId()))
                .andExpect(jsonPath("$.verification_id").value(REQUEST.verificationId()))
                .andRespond(withSuccess(SUCCESS_JSON, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.verify(REQUEST))
                .isInstanceOf(PhotoVerificationTechnicalException.class)
                .hasRootCauseInstanceOf(SocketTimeoutException.class);
        assertThat(service.verify(REQUEST).status()).isEqualTo(VerificationResult.SUCCESS);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "ai-server", "file:///tmp/ai", "http://ai.test?x=y", "http://ai.test#fragment"})
    void invalidConfigurationDoesNotSendRequest(String url) {
        assertThatThrownBy(() -> serviceFor(url).verify(REQUEST))
                .isInstanceOf(PhotoVerificationTechnicalException.class)
                .hasMessage("AI_SERVER_URL 설정이 필요합니다.");
    }

    @Test
    void acceptsServerUrlWithTrailingSlash() {
        respond(SUCCESS_JSON);

        assertThat(serviceFor(SERVER_URL + "/").verify(REQUEST).status()).isEqualTo(VerificationResult.SUCCESS);
    }

    @ParameterizedTest
    @CsvSource({"true,true", "true,false", "false,true"})
    void invalidRequestAcceptsUnreadableIds(boolean nullTask, boolean nullVerification) throws Exception {
        ObjectNode body = (ObjectNode) MAPPER.readTree(errorJson("INVALID_REQUEST"));
        if (nullTask) body.putNull("ai_task_id");
        if (nullVerification) body.putNull("verification_id");
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON).body(body.toString()));

        assertThatThrownBy(() -> service.verify(REQUEST))
                .isInstanceOf(PhotoVerificationTechnicalException.class)
                .hasMessage("AI 판정 기술 오류: INVALID_REQUEST");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ai_task_id", "verification_id"})
    void invalidRequestStillRejectsNonMatchingIds(String field) throws Exception {
        for (String value : List.of("wrong-id", "", " ")) {
            ObjectNode body = (ObjectNode) MAPPER.readTree(errorJson("INVALID_REQUEST"));
            body.putNull("ai_task_id");
            body.putNull("verification_id");
            body.put(field, value);
            server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON).body(body.toString()));
        }
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.verify(REQUEST))
                    .isInstanceOf(PhotoVerificationTechnicalException.class)
                    .hasMessage("AI 판정 요청과 응답의 ID가 일치하지 않습니다.");
        }
    }

    @ParameterizedTest
    @CsvSource({"422,UNSUPPORTED_IMAGE", "422,IMAGE_FETCH_FAILED",
            "500,INTERNAL_ERROR", "503,MODEL_UNAVAILABLE"})
    void otherErrorsStillRequireBothIds(int status, String code) throws Exception {
        for (String field : List.of("ai_task_id", "verification_id")) {
            ObjectNode body = (ObjectNode) MAPPER.readTree(errorJson(code));
            body.putNull(field);
            server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.valueOf(status))
                    .contentType(MediaType.APPLICATION_JSON).body(body.toString()));
        }
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> service.verify(REQUEST))
                    .isInstanceOf(PhotoVerificationTechnicalException.class)
                    .hasMessage("AI 판정 요청과 응답의 ID가 일치하지 않습니다.");
        }
    }

    private PhotoVerificationService serviceFor(String serverUrl) {
        return new PhotoVerificationService(new PhotoVerificationClient(
                restClient, MAPPER, VALIDATORS.getValidator(), serverUrl));
    }

    private void respond(String body) {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private static ObjectNode success() throws Exception {
        return (ObjectNode) MAPPER.readTree(SUCCESS_JSON);
    }

    private static Arguments changed(String description, Consumer<ObjectNode> change) throws Exception {
        ObjectNode response = success();
        change.accept(response);
        return Arguments.of(description, response.toString());
    }

    private static String errorJson(String code) {
        return """
                {
                  "ai_task_id": "9007199254740993",
                  "verification_id": "22",
                  "processing_status": "ERROR",
                  "error": {"code": "%s", "message": "모델 사용 불가"}
                }
                """.formatted(code);
    }
}
