package com.tropical.backend.calendar.client;

import com.fasterxml.jackson.databind.JsonNode;
import io.netty.channel.ChannelOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * 한국천문연구원 공휴일 API 클라이언트.
 *
 * <p>
 * WebClient를 사용하여 한국천문연구원의 특일 정보 API와 연동합니다.
 * 공휴일, 국경일 등의 데이터를 JSON 형식으로 수집하여 제공합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>월별 공휴일 조회 (getRestDeInfo 엔드포인트)</li>
 *   <li>월별 국경일 조회 (getHoliDeInfo 엔드포인트)</li>
 *   <li>네트워크 레벨(연결/응답) 및 리액티브 체인 이중 타임아웃 설정</li>
 *   <li>일시적 장애 대응을 위한 경량 재시도 메커니즘</li>
 *   <li>입력 파라미터 검증 및 에러 상황 처리</li>
 *   <li>상세 로깅 및 예외 처리</li>
 * </ul>
 *
 * @author  왕택준
 * @version 0.1
 * @since   2025.09.16
 */
@Slf4j
@Component
public class HolidayApiClient {

    private final WebClient webClient;
    private final String apiKey;
    private final int pageNo;
    private final int numOfRows;

    /**
     * HolidayApiClient 생성자.
     *
     * <p>
     * 환경 설정 값들을 주입받아 WebClient를 초기화합니다.
     * 네트워크 레벨의 연결/응답 타임아웃과 기본 헤더를 설정하여 안정적인 API 연동을 보장합니다.
     * User-Agent 헤더를 포함하여 일부 공공 API에서 발생할 수 있는 접근 제한을 방지합니다.
     * </p>
     *
     * @param apiBase API 베이스 URL (예: https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService)
     * @param apiKey 공공데이터포털에서 발급받은 서비스 키 (디코딩된 키 사용 권장)
     * @param connectTimeoutMs 연결 타임아웃 (밀리초, 기본값: 3000)
     * @param readTimeoutMs 응답 타임아웃 (밀리초, 기본값: 4000)
     * @param pageNo API 호출 시 사용할 페이지 번호 (기본값: 1)
     * @param numOfRows 한 번에 조회할 최대 행 수 (기본값: 100, 월별 특일 수 고려)
     */
    public HolidayApiClient(
            @Value("${holiday.api-base}") String apiBase,
            @Value("${holiday.api-key}") String apiKey,
            @Value("${holiday.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${holiday.read-timeout-ms:4000}") int readTimeoutMs,
            @Value("${holiday.page-no:1}") int pageNo,
            @Value("${holiday.num-of-rows:100}") int numOfRows
    ) {
        this.apiKey = apiKey;
        this.pageNo = pageNo;
        this.numOfRows = numOfRows;

        // 1. 네트워크 레벨 타임아웃 설정
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs) // 연결 타임아웃
                .responseTimeout(Duration.ofMillis(readTimeoutMs));             // 응답 타임아웃 (데이터 수신 전체 시간)

        this.webClient = WebClient.builder()
                .baseUrl(apiBase)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "tropical-backend/1.0")
                .build();

        log.info("HolidayApiClient 초기화 완료 - baseUrl: {}, connectTimeout: {}ms, readTimeout: {}ms, pageNo: {}, numOfRows: {}",
                apiBase, connectTimeoutMs, readTimeoutMs, pageNo, numOfRows);
    }

    /**
     * 월별 공휴일을 조회합니다.
     *
     * <p>
     * getRestDeInfo 엔드포인트를 호출하여 법정 공휴일 및 대체공휴일 정보를 가져옵니다.
     * 조회된 데이터에는 신정, 설날, 어린이날, 현충일, 광복절, 개천절, 한글날, 성탄절 등이 포함됩니다.
     * </p>
     *
     * @param year 조회할 연도 (1900년 이후, 예: 2025)
     * @param month 조회할 월 (1-12)
     * @return JSON 응답 데이터 (JsonNode), null이 아님을 보장
     * @throws IllegalArgumentException 연도나 월이 유효하지 않은 경우
     * @throws RuntimeException API 호출 실패 시
     */
    public JsonNode fetchRestHolidays(int year, int month) {
        log.debug("공휴일 조회 시작 - year: {}, month: {}", year, month);
        validateParameters(year, month);
        return callApi("/getRestDeInfo", year, month);
    }

    /**
     * 월별 국경일을 조회합니다.
     *
     * <p>
     * getHoliDeInfo 엔드포인트를 호출하여 국경일 정보를 가져옵니다.
     * 조회된 데이터에는 3.1절, 제헌절, 광복절, 개천절, 한글날 등의 국경일이 포함됩니다.
     * </p>
     *
     * <p>공휴일과 국경일의 차이:</p>
     * <ul>
     *   <li>공휴일(RestDe): 법정 공휴일로 지정되어 쉬는 날</li>
     *   <li>국경일(HoliDe): 국가적 의미를 가진 기념일 (공휴일이 아닐 수도 있음)</li>
     * </ul>
     *
     * <p>데이터 없음 처리:</p>
     * <p>해당 월에 국경일이 없는 경우 정상적인 상황으로 간주하여
     * 적절한 응답 구조를 가진 빈 데이터를 반환합니다.</p>
     *
     * @param year 조회할 연도 (1900년 이후)
     * @param month 조회할 월 (1-12)
     * @return JSON 응답 데이터 (JsonNode), null이 아님을 보장
     * @throws IllegalArgumentException 연도나 월이 유효하지 않은 경우
     * @throws RuntimeException API 호출 실패 시
     */
    public JsonNode fetchNationalHolidays(int year, int month) {
        log.debug("국경일 조회 시작 - year: {}, month: {}", year, month);
        validateParameters(year, month);
        return callApi("/getHoliDeInfo", year, month);
    }

    /**
     * 연도와 월 파라미터의 유효성을 검증합니다.
     *
     * <p>
     * API 호출 전에 입력 파라미터가 올바른 범위에 있는지 확인합니다.
     * 잘못된 파라미터로 인한 불필요한 API 호출을 방지하고,
     * 명확한 에러 메시지를 통해 디버깅을 용이하게 합니다.
     * </p>
     *
     * @param year 검증할 연도
     * @param month 검증할 월
     * @throws IllegalArgumentException 연도가 1900년 미만이거나 월이 1-12 범위를 벗어난 경우
     */
    private void validateParameters(int year, int month) {
        if (year < 1900) {
            throw new IllegalArgumentException("year는 1900년 이상이어야 합니다. 입력된 값: " + year);
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month는 1~12 사이여야 합니다. 입력된 값: " + month);
        }
    }

    /**
     * 공통 API 호출 및 응답 처리 메서드.
     *
     * <p>
     * 공휴일 API의 공통 파라미터를 설정하고 HTTP GET 요청을 동기 방식으로 수행합니다.
     * HTTP 에러 및 API 자체 에러 코드(resultCode)를 검증하고, 실패 시 예외를 발생시킵니다.
     * </p>
     *
     * <p>처리 과정:</p>
     * <ol>
     *   <li>URI 빌더를 통한 쿼리 파라미터 구성 (서비스 키 자동 인코딩)</li>
     *   <li>WebClient를 사용한 비동기 HTTP GET 요청</li>
     *   <li>HTTP 상태 코드 검증 (4xx/5xx 에러 처리)</li>
     *   <li>JSON 응답 파싱</li>
     *   <li>일시적 장애에 대한 재시도 (최대 2회, 300ms 간격)</li>
     *   <li>리액티브 체인 타임아웃 적용 (10초)</li>
     *   <li>동기 블로킹으로 결과 반환</li>
     *   <li>비즈니스 레벨 성공 여부 검증</li>
     * </ol>
     *
     * @param path API 엔드포인트 경로 (/getRestDeInfo 또는 /getHoliDeInfo)
     * @param year 조회할 연도
     * @param month 조회할 월
     * @return 파싱된 JSON 응답 (null이 아님을 보장)
     * @throws RuntimeException 네트워크 오류, 타임아웃, HTTP 에러, API 비즈니스 에러 시
     */
    private JsonNode callApi(String path, int year, int month) {
        String monthStr = String.format("%02d", month);

        try {
            JsonNode rootNode = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("serviceKey", apiKey)  // WebClient가 자동으로 URL 인코딩 처리
                            .queryParam("solYear", year)
                            .queryParam("solMonth", monthStr)
                            .queryParam("_type", "json")      // JSON 응답 형식 지정
                            .queryParam("pageNo", pageNo)     // 페이지 번호 (설정 가능)
                            .queryParam("numOfRows", numOfRows) // 최대 행 수 (설정 가능)
                            .build())
                    .retrieve()
                    // HTTP 상태 코드가 4xx 또는 5xx인 경우 처리
                    .onStatus(
                            status -> status.isError(),
                            response -> response.bodyToMono(String.class)
                                    .flatMap(errorBody -> {
                                        log.warn("KASI API HTTP 오류 발생 - status: {}, path: {}, year: {}, month: {}, body: {}",
                                                response.statusCode(), path, year, month,
                                                errorBody.length() > 200 ? errorBody.substring(0, 200) + "..." : errorBody);
                                        return Mono.error(new RuntimeException(
                                                "KASI API 호출 실패: HTTP " + response.statusCode()
                                        ));
                                    })
                    )
                    .bodyToMono(JsonNode.class)
                    // 일시적 장애에 대한 경량 재시도 메커니�m
                    .retryWhen(
                            Retry.fixedDelay(2, Duration.ofMillis(300))
                                    .filter(throwable -> {
                                        // 재시도할 예외 타입 선별
                                        String exceptionName = throwable.getClass().getSimpleName();
                                        boolean shouldRetry = exceptionName.contains("Timeout") ||
                                                              exceptionName.contains("Connect") ||
                                                              throwable.getMessage() != null && throwable.getMessage().contains("5");

                                        if (shouldRetry) {
                                            log.debug("API 호출 재시도 - path: {}, year: {}, month: {}, exception: {}",
                                                    path, year, month, exceptionName);
                                        }
                                        return shouldRetry;
                                    })
                                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                        log.warn("API 호출 재시도 한계 초과 - path: {}, year: {}, month: {}, attempts: {}",
                                                path, year, month, retrySignal.totalRetries() + 1);
                                        return retrySignal.failure();
                                    })
                    )
                    // 리액티브 체인 레벨 타임아웃 (전체 과정 시간 제한)
                    .timeout(Duration.ofSeconds(10))
                    .block(); // 비동기 결과를 동기적으로 대기

            // API 응답 내용의 성공 여부 검증
            ensureSuccess(rootNode, path, year, month);
            return rootNode;

        } catch (Exception e) {
            // WebClientResponseException인 경우 HTTP 상태 코드 정보 포함
            if (e instanceof WebClientResponseException webEx) {
                log.error("API 호출 중 WebClient 예외 발생 - path: {}, year: {}, month: {}, status: {}, error: {}",
                        path, year, month, webEx.getStatusCode(), webEx.getMessage(), e);
                throw new RuntimeException("공휴일 API(" + path + ") 호출 실패: " + webEx.getStatusCode(), e);
            } else {
                log.error("API 호출 중 예외 발생 - path: {}, year: {}, month: {}, error: {}",
                        path, year, month, e.getMessage(), e);
                throw new RuntimeException("공휴일 API(" + path + ") 호출에 실패했습니다.", e);
            }
        }
    }

    /**
     * KASI API 응답의 비즈니스 레벨 성공 여부를 검증합니다.
     *
     * <p>
     * HTTP 상태 코드가 200이라도, 응답 본문의 resultCode가 성공 코드가 아니면 실패로 간주합니다.
     * 공공데이터포털 API의 표준 에러 코드 체계를 따라 응답을 검증합니다.
     * </p>
     *
     * <p>주요 resultCode 처리:</p>
     * <ul>
     *   <li><b>00</b>: 정상 처리 (성공)</li>
     *   <li><b>03</b>: 데이터 없음 (정상 상황으로 간주, 예외 발생 안함)</li>
     *   <li><b>기타</b>: 에러 상황으로 간주하여 RuntimeException 발생</li>
     * </ul>
     *
     * @param rootNode API 응답의 최상위 JsonNode
     * @param path 호출한 API 경로 (로깅용)
     * @param year 호출한 연도 (로깅용)
     * @param month 호출한 월 (로깅용)
     * @throws RuntimeException 응답이 null이거나 resultCode가 에러 코드인 경우
     */
    private void ensureSuccess(JsonNode rootNode, String path, int year, int month) {
        if (rootNode == null) {
            throw new RuntimeException("KASI API 응답이 null입니다: " + path + " " + year + "-" + month);
        }

        JsonNode header = rootNode.path("response").path("header");
        String resultCode = header.path("resultCode").asText(null);

        // 데이터 없음(03)은 정상 상황으로 간주
        if ("03".equals(resultCode)) {
            log.debug("KASI API 데이터 없음(03) - path: {}, year: {}, month: {} (정상 처리)", path, year, month);
            return; // 예외 없이 통과, 파서에서 빈 목록 처리됨
        }

        // 정상(00) 이외의 코드는 에러로 처리
        if (!"00".equals(resultCode)) {
            String resultMsg = header.path("resultMsg").asText("알 수 없는 오류");
            String errorMessage = String.format("KASI API가 실패 응답을 반환했습니다. (코드: %s, 메시지: %s) [%s %d-%d]",
                    resultCode, resultMsg, path, year, month);
            log.warn(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        log.debug("API 호출 성공 - path: {}, year: {}, month: {}", path, year, month);
    }
}