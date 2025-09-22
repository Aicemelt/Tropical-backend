package com.tropical.backend.calendar.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Clock;
import java.time.Duration;
import java.time.Year;

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
 *   <li>월별 공휴일 조회 (getRestDeInfo)</li>
 *   <li>월별 국경일 조회 (getHoliDeInfo)</li>
 *   <li>네트워크/리액티브 타임아웃 + 경량 재시도</li>
 *   <li><b>동적 소프트가드</b>: 현재 연도 + aheadYears 초과 시 호출 생략</li>
 *   <li><b>4xx 무해화</b>: 4xx 응답은 예외 대신 빈 응답 반환</li>
 *   <li>상세 로깅 및 비즈니스 코드 검증</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.2
 * @since 2025.09.16
 */
@Slf4j
@Component
public class HolidayApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final WebClient webClient;
    private final String apiKey;
    private final int pageNo;
    private final int numOfRows;
    private final Clock clock;

    public HolidayApiClient(
            @Value("${holiday.api-base}") String apiBase,
            @Value("${holiday.api-key}") String apiKey,
            @Value("${holiday.connect-timeout-ms:3000}") int connectTimeoutMs,
            @Value("${holiday.read-timeout-ms:4000}") int readTimeoutMs,
            @Value("${holiday.page-no:1}") int pageNo,
            @Value("${holiday.num-of-rows:100}") int numOfRows,
            Clock clock
    ) {
        this.apiKey = apiKey;
        this.pageNo = pageNo;
        this.numOfRows = numOfRows;
        this.clock = clock;

        // 1) 네트워크 레벨 타임아웃
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
                .responseTimeout(Duration.ofMillis(readTimeoutMs));

        // 2) WebClient 기본 설정
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
     * @param year  조회할 연도 (1900년 이후, 예: 2025)
     * @param month 조회할 월 (1-12)
     * @return JSON 응답 데이터 (JsonNode), null이 아님을 보장
     * @throws IllegalArgumentException 연도나 월이 유효하지 않은 경우
     * @throws RuntimeException         API 호출 실패 시
     */
    public JsonNode fetchRestHolidays(int year, int month) {
        log.debug("공휴일 조회 시작 - year: {}, month: {}", year, month);
        validateParameters(year, month);

        // **동적 소프트가드**: 현재년도 + aheadYears 초과 시 호출 생략
        if (isBeyondFutureLimit(year)) {
            log.info("공휴일 조회 생략(소프트가드) - year: {} (최대 허용: {}), month: {}",
                    year, getMaxAllowedYear(), month);
            return emptyResponse("getRestDeInfo", year, month);
        }

        return callApi("/getRestDeInfo", year, month);
    }

    /**
     * 월별 국경일 조회.
     *
     * @param year  1900+
     * @param month 1-12
     */
    public JsonNode fetchNationalHolidays(int year, int month) {
        log.debug("국경일 조회 시작 - year: {}, month: {}", year, month);
        validateParameters(year, month);

        // **동적 소프트가드**
        if (isBeyondFutureLimit(year)) {
            log.info("국경일 조회 생략(소프트가드) - year: {} (최대 허용: {}), month: {}",
                    year, getMaxAllowedYear(), month);
            return emptyResponse("getHoliDeInfo", year, month);
        }

        return callApi("/getHoliDeInfo", year, month);
    }

    /**
     * 현재년도 + TimeConfig.HOLIDAY_AHEAD_YEARS.
     */
    private int getMaxAllowedYear() {
        return Year.now(clock).getValue() + com.tropical.backend.config.TimeConfig.HOLIDAY_AHEAD_YEARS;
    }

    /**
     * 연도가 상한을 초과하는지 여부.
     */
    private boolean isBeyondFutureLimit(int year) {
        return year > getMaxAllowedYear();
    }

    /**
     * 파라미터 기본 검증.
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
     * 공통 호출부.
     *
     * <p><b>4xx 무해화:</b> 4xx 응답은 예외 대신 빈 응답을 반환합니다.</p>
     * <p>5xx·타임아웃·연결류만 재시도 유지.</p>
     */
    private JsonNode callApi(String path, int year, int month) {
        String monthStr = String.format("%02d", month);

        try {
            JsonNode rootNode = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                            .queryParam("serviceKey", apiKey)
                            .queryParam("solYear", year)
                            .queryParam("solMonth", monthStr)
                            .queryParam("_type", "json")
                            .queryParam("pageNo", pageNo)
                            .queryParam("numOfRows", numOfRows)
                            .build())
                    // retrieve().onStatus(...)는 4xx를 성공 흐름으로 바꾸기 어렵다.
                    // exchangeToMono로 분기 처리하여 4xx는 "빈 응답" 반환.
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(JsonNode.class);
                        }
                        if (response.statusCode().is4xxClientError()) {
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        log.warn("KASI API 4xx 응답 무해화 - status: {}, path: {}, y: {}, m: {}, body: {}",
                                                response.statusCode(), path, year, month,
                                                body.length() > 200 ? body.substring(0, 200) + "..." : body);
                                        return emptyResponse(path, year, month);
                                    });
                        }
                        // 5xx는 예외로 전파해서 재시도 타게 함
                        return response.createException().flatMap(Mono::error);
                    })
                    // 재시도: 5xx/Timeout/Connect류만
                    .retryWhen(
                            Retry.fixedDelay(2, Duration.ofMillis(300))
                                    .filter(throwable -> {
                                        if (throwable instanceof WebClientResponseException ex) {
                                            // 5xx만 재시도
                                            return ex.getStatusCode().is5xxServerError();
                                        }
                                        String name = throwable.getClass().getSimpleName();
                                        return name.contains("Timeout") || name.contains("Connect");
                                    })
                                    .onRetryExhaustedThrow((spec, signal) -> signal.failure())
                    )
                    .timeout(Duration.ofSeconds(10))
                    .block();

            ensureSuccess(rootNode, path, year, month);
            return rootNode;

        } catch (Exception e) {
            if (e instanceof WebClientResponseException webEx) {
                log.error("API 호출 중 WebClient 예외 - path: {}, year: {}, month: {}, status: {}, error: {}",
                        path, year, month, webEx.getStatusCode(), webEx.getMessage(), e);
                throw new RuntimeException("공휴일 API(" + path + ") 호출 실패: " + webEx.getStatusCode(), e);
            } else {
                log.error("API 호출 중 예외 - path: {}, year: {}, month: {}, error: {}",
                        path, year, month, e.getMessage(), e);
                throw new RuntimeException("공휴일 API(" + path + ") 호출에 실패했습니다.", e);
            }
        }
    }

    /**
     * 응답 비즈니스 성공 여부 검증.
     *
     * <p>resultCode:</p>
     * <ul>
     *   <li>00: 정상</li>
     *   <li>03: 데이터 없음(정상으로 간주)</li>
     *   <li>기타: 실패</li>
     * </ul>
     */
    private void ensureSuccess(JsonNode rootNode, String path, int year, int month) {
        if (rootNode == null) {
            throw new RuntimeException("KASI API 응답이 null입니다: " + path + " " + year + "-" + month);
        }

        JsonNode header = rootNode.path("response").path("header");
        String resultCode = header.path("resultCode").asText(null);

        if ("03".equals(resultCode)) {
            log.debug("KASI API 데이터 없음(03) - path: {}, year: {}, month: {} (정상 처리)", path, year, month);
            return;
        }
        if (!"00".equals(resultCode)) {
            String resultMsg = header.path("resultMsg").asText("알 수 없는 오류");
            String msg = String.format("KASI API 실패 응답 (코드: %s, 메시지: %s) [%s %d-%d]",
                    resultCode, resultMsg, path, year, month);
            log.warn(msg);
            throw new RuntimeException(msg);
        }

        log.debug("API 호출 성공 - path: {}, year: {}, month: {}", path, year, month);
    }

    /**
     * “빈 목록”을 의미하는 표준 형태의 응답 페이로드 생성.
     *
     * <p>
     * - resultCode는 '03'(데이터 없음)으로 설정<br>
     * - 파서가 안전하게 빈 배열로 처리 가능하도록 구조 유지
     * </p>
     */
    private JsonNode emptyResponse(String path, int year, int month) {
        try {
            String json = """
                    {
                      "response": {
                        "header": { "resultCode": "03", "resultMsg": "NO DATA" },
                        "body": {
                          "items": { "item": [] },
                          "totalCount": 0,
                          "pageNo": %d,
                          "numOfRows": %d
                        }
                      },
                      "_meta": { "path": "%s", "year": %d, "month": %d }
                    }
                    """.formatted(pageNo, numOfRows, path, year, month);
            return MAPPER.readTree(json);
        } catch (Exception e) {
            // 만약 파싱에 실패하면 최소 구조라도 만들어 반환
            log.warn("emptyResponse 생성 중 예외: {}", e.getMessage());
            return MAPPER.createObjectNode()
                    .putObject("response").putObject("header")
                    .put("resultCode", "03").put("resultMsg", "NO DATA");
        }
    }
}