package com.tropical.backend.calendar.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.tropical.backend.calendar.client.HolidayApiClient;
import com.tropical.backend.calendar.dto.response.DayOffStatusResponse;
import com.tropical.backend.calendar.dto.response.HolidayCheckResponse;
import com.tropical.backend.calendar.dto.response.HolidayEventResponse;
import com.tropical.backend.calendar.entity.Holiday;
import com.tropical.backend.calendar.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 공휴일 서비스.
 *
 * <p>
 * 공휴일 데이터의 조회, 캐싱, 외부 API 연동을 담당하는 비즈니스 로직 계층입니다.
 * <b>캐시 우선 전략</b>을 사용하여 불필요한 외부 API 호출을 최소화하고,
 * DB에서 최종 조회하여 데이터의 일관성을 보장합니다.
 * </p>
 *
 * <p>주요 기능:</p>
 * <ul>
 *   <li>월별 공휴일 조회 (캐시 우선, 없으면 API 호출 후 저장 및 재조회)</li>
 *   <li>특정 날짜가 실제 휴일인지 여부 판단 (24절기, 기념일 제외)</li>
 *   <li>API JSON 응답을 Holiday 엔터티로 변환 및 타입 매핑</li>
 *   <li>중복 저장 방지 로직 수행 (동일한 날짜/이름/국가 조합 검증)</li>
 *   <li>API 장애 시 graceful fallback (빈 리스트 반환)</li>
 * </ul>
 *
 * @author 왕택준
 * @version 0.1
 * @since 2025.09.16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HolidayService {

    // ===============================
    // 상수 / 포맷터
    // ===============================

    private static final int MIN_YEAR = 1900; // 최소 년도

    /**
     * 한국천문연구원 API의 locdate 필드(yyyyMMdd) 파싱용 포맷터.
     */
    private static final DateTimeFormatter LOCDATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    /**
     * 고정 국가 코드 (한국).
     */
    private static final String COUNTRY_CODE_KR = "KR";

    /**
     * API 소스 타입: 공휴일 API (getRestDeInfo 엔드포인트).
     */
    private static final String SOURCE_TYPE_REST = "REST";

    /**
     * API 소스 타입: 국경일 API (getHoliDeInfo 엔드포인트).
     */
    private static final String SOURCE_TYPE_NATIONAL = "NATIONAL";

    /**
     * 실제 휴무일로 인정하는 타입 집합(법정/국경/대체공휴일).
     */
    private static final Set<Holiday.HolidayType> ACTUAL_TYPES = EnumSet.of(
            Holiday.HolidayType.PUBLIC_HOLIDAY,
            Holiday.HolidayType.NATIONAL_HOLIDAY,
            Holiday.HolidayType.SUBSTITUTE_HOLIDAY
    );

    // ===============================
    // 의존성
    // ===============================

    private final Clock clock; // 현재 년도
    private final HolidayApiClient holidayApiClient;
    private final HolidayRepository holidayRepository;

    // ===============================
    // 퍼블릭 서비스 API
    // ===============================

    /**
     * 월별 공휴일 조회.
     *
     * <p>
     * 캐시 우선 전략을 사용하며, 데이터 일관성을 위해 다음 순서로 동작합니다:
     * </p>
     *
     * <ol>
     *   <li><b>입력 검증</b>: 연도와 월 파라미터의 유효성을 확인합니다.</li>
     *   <li><b>캐시 확인</b>: DB에서 해당 월 범위의 공휴일을 조회합니다.</li>
     *   <li><b>캐시 히트</b>: 기존 데이터가 있으면 즉시 반환합니다.</li>
     *   <li><b>API 호출</b>: 캐시 미스 시 외부 API를 호출하여 공휴일과 국경일을 각각 수집합니다.</li>
     *   <li><b>데이터 파싱</b>: JSON 응답을 Holiday 엔터티로 변환합니다.</li>
     *   <li><b>중복 제거 저장</b>: DB에 없는 새로운 공휴일만 선별하여 저장합니다.</li>
     *   <li><b>최종 조회</b>: DB에서 다시 조회하여 트랜잭션 일관성이 보장된 데이터를 반환합니다.</li>
     * </ol>
     *
     * @param year  조회할 연도 (1900년 이후)
     * @param month 조회할 월 (1-12)
     * @return 해당 월의 공휴일 목록 (정렬 순서는 Repository에서 결정)
     * @throws IllegalArgumentException 연도나 월이 유효하지 않은 경우
     */
    @Transactional
    public List<Holiday> getMonthlyHolidays(int year, int month) {
        // 1. 입력 파라미터 검증
        validateYearMonth(year, month);
        validateYear(year);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        log.debug("월별 공휴일 조회 시작 - year: {}, month: {} ({}~{})", year, month, monthStart, monthEnd);

        // 2. 캐시에서 조회 (해당 월과 겹치는 기간의 공휴일 검색)
        List<Holiday> cachedHolidays = holidayRepository.findOverlappingHolidays(COUNTRY_CODE_KR, monthStart, monthEnd);
        if (!cachedHolidays.isEmpty()) {
            log.info("캐시에서 공휴일 발견 - year: {}, month: {}, 건수: {}", year, month, cachedHolidays.size());
            return cachedHolidays;
        }

        log.debug("캐시에 데이터 없음. API를 통해 데이터 수집 시작 - year: {}, month: {}", year, month);

        // 3. 외부 API에서 데이터 수집
        List<Holiday> collectedHolidays;
        try {
            // 공휴일 API와 국경일 API를 각각 호출
            JsonNode restResponse = holidayApiClient.fetchRestHolidays(year, month);
            JsonNode nationalResponse = holidayApiClient.fetchNationalHolidays(year, month);

            // 두 응답을 모두 파싱하여 합침
            List<Holiday> parsedHolidays = new ArrayList<>();
            parsedHolidays.addAll(parseHolidayResponse(restResponse, SOURCE_TYPE_REST));
            parsedHolidays.addAll(parseHolidayResponse(nationalResponse, SOURCE_TYPE_NATIONAL));

            // 동일 날짜 중복 제거 (우선순위: 대체공휴일 > 법정공휴일 > 국경일)
            parsedHolidays = dedupByDateWithPriority(parsedHolidays);
            collectedHolidays = parsedHolidays;

            log.debug("API에서 수집된 특일 - 건수: {}", collectedHolidays.size());

        } catch (Exception e) {
            log.error("공휴일 API 호출 중 실패 - year: {}, month: {}, error: {}", year, month, e.getMessage(), e);
            return Collections.emptyList(); // 장애 상황에서도 빈 리스트를 반환하여 서비스 안정성 확보
        }

        // 4. 중복 방지 후 저장
        saveNewHolidays(collectedHolidays, year, month);

        // 5. 최종적으로 DB에서 다시 조회하여 트랜잭션 일관성이 보장된 데이터를 반환
        return holidayRepository.findOverlappingHolidays(COUNTRY_CODE_KR, monthStart, monthEnd);
    }

    /**
     * 특정 날짜가 실제 공휴일(휴무일)인지 확인합니다.
     *
     * <p>
     * DB 조회 시점에서 {@link #ACTUAL_TYPES} (법정공휴일, 국경일, 대체공휴일)만 대상으로 검색합니다.
     * 따라서 24절기, 기념일, 전통 기념일 등은 애초에 조회되지 않으며,
     * 결과가 존재하면 휴무일로 판정합니다.
     * 주말과의 조합 검사는 별도로 수행해야 합니다.
     * </p>
     *
     * <p>공휴일로 인정하는 타입:</p>
     * <ul>
     *   <li>NATIONAL_HOLIDAY: 국경일 (3.1절, 광복절, 개천절, 한글날)</li>
     *   <li>PUBLIC_HOLIDAY: 법정 공휴일 (신정, 설날, 추석, 어린이날, 부처님오신날, 현충일, 성탄절)</li>
     *   <li>SUBSTITUTE_HOLIDAY: 대체공휴일 (공휴일이 주말과 겹칠 때 지정)</li>
     * </ul>
     *
     * <p>공휴일로 인정하지 않는 타입:</p>
     * <ul>
     *   <li>MEMORIAL_DAY: 기념일 (어버이날, 스승의날 등)</li>
     *   <li>SEASONAL_DIVISION: 24절기 (입춘, 하지, 추분 등)</li>
     *   <li>TRADITIONAL_DAY: 전통 기념일</li>
     * </ul>
     *
     * @param date 확인할 날짜 (null 불가)
     * @return 실제 공휴일이면 {@code true}, 아니면 {@code false}
     * @throws IllegalArgumentException date가 null인 경우
     */
    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("date는 null일 수 없습니다.");
        // validateYear(date); // 필요 시 활성화

        List<Holiday> hits = holidayRepository.findOnDateByTypes(COUNTRY_CODE_KR, date, ACTUAL_TYPES);
        boolean isHoliday = !hits.isEmpty();

        log.debug("공휴일 여부 확인 - date: {}, isHoliday: {}, 매칭된 특일 수: {}", date, isHoliday, hits.size());
        return isHoliday;
    }

    /**
     * 특정 날짜의 실제 공휴일 이름을 반환합니다.
     *
     * <p>DB 1차 조회 → 캐시 미스 시 해당 월 데이터 워밍업(getMonthlyHolidays) → 재조회.</p>
     * <p><b>주의:</b> 워밍업 과정에서 쓰기가 발생하므로 readOnly=false.</p>
     *
     * <p>
     * 내부적으로 DB에서 해당 날짜에 포함되는 특일을 조회한 뒤,
     * 실제 휴무일로 간주하는 타입({@code ACTUAL_TYPES})만 필터링하여
     * 한국어 공휴일명(nameKo)을 반환합니다.
     * </p>
     *
     * @param date 확인할 날짜 (null 불가)
     * @return 공휴일명이 있으면 Optional(name), 없으면 Optional.empty()
     * @throws IllegalArgumentException date가 null이거나 연도 범위를 벗어난 경우
     */
    @Transactional
    public Optional<String> getActualHolidayName(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("date는 null일 수 없습니다.");
        validateYear(date);

        // 1차: DB 히트 시 즉시 반환
        Optional<String> name = findActualHolidayNameInDb(date);
        if (name.isPresent()) return name;

        // 2차: 캐시 미스 → 해당 월 데이터 워밍업 후 재조회
        YearMonth ym = YearMonth.from(date);
        getMonthlyHolidays(ym.getYear(), ym.getMonthValue());

        return findActualHolidayNameInDb(date);
    }

    /**
     * DB에서 실제 휴무일 타입만 대상으로 공휴일명을 조회합니다.
     *
     * @param date 조회 날짜
     * @return Optional(nameKo)
     */
    @Transactional(readOnly = true)
    private Optional<String> findActualHolidayNameInDb(LocalDate date) {
        return holidayRepository
                .findOnDateByTypes(COUNTRY_CODE_KR, date, ACTUAL_TYPES)
                .stream()
                .map(Holiday::getNameKo)
                .filter(Objects::nonNull)
                .findFirst();
    }

    /**
     * 특정 날짜의 휴일 상태를 DTO로 반환합니다.
     *
     * <p>
     * 공휴일명이 존재하면 {@link HolidayCheckResponse#holiday(LocalDate, String)} 형태로,
     * 아니면 {@link HolidayCheckResponse#notHoliday(LocalDate)} 형태로 응답을 구성합니다.
     * </p>
     *
     * @param date 확인할 날짜
     * @return HolidayCheckResponse (isHoliday, date, holidayName 포함)
     */
    @Transactional(readOnly = true)
    public HolidayCheckResponse checkHolidayStatus(LocalDate date) {
        Optional<String> name = getActualHolidayName(date);
        return name.map(n -> HolidayCheckResponse.holiday(date, n))
                .orElseGet(() -> HolidayCheckResponse.notHoliday(date));
    }

    /**
     * 주말(토/일) 여부를 반환합니다.
     *
     * @param date 확인할 날짜
     * @return 토요일 또는 일요일이면 {@code true}
     */
    private boolean isWeekend(LocalDate date) {
        return switch (date.getDayOfWeek()) {
            case SATURDAY, SUNDAY -> true;
            default -> false;
        };
    }

    /**
     * 특정 날짜가 "휴무일(빨간날)"인지 확인합니다.
     *
     * <p>
     * 주말(토/일) 또는 실제 공휴일(법정/국경/대체) 중 하나라도 만족하면 {@code true}를 반환합니다.
     * 캘린더 UI에서 빨간색 표시 여부를 위한 통합 판정 메서드입니다.
     * </p>
     *
     * @param date 확인할 날짜 (null 불가)
     * @return 주말이거나 공휴일이면 {@code true}
     * @throws IllegalArgumentException date가 null인 경우
     */
    @Transactional(readOnly = true)
    public boolean isDayOff(LocalDate date) {
        if (date == null) throw new IllegalArgumentException("date는 null일 수 없습니다.");
        validateYear(date);

        if (isWeekend(date)) return true;
        return isHoliday(date);
    }

    /**
     * 월 단위로 주말/공휴일/휴무일 상태를 반환합니다.
     *
     * <p>
     * 해당 월(1일 ~ 말일)을 순회하며 각 날짜의 상태를 생성합니다.
     * 공휴일명은 실제 휴무일일 때만 채워집니다. 캘린더 UI에서
     * 월별 날짜 상태 표시에 활용할 수 있습니다.
     * </p>
     *
     * @param year  연도 (1900년 이후)
     * @param month 월 (1-12)
     * @return DayOffStatus 리스트 (1일~말일 순서)
     * @throws IllegalArgumentException 연도나 월이 유효하지 않은 경우
     */
    @Transactional(readOnly = true)
    public List<DayOffStatusResponse> getMonthDayOffStatus(int year, int month) {
        validateYearMonth(year, month);
        validateYear(year);

        YearMonth ym = YearMonth.of(year, month);
        LocalDate d = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        List<DayOffStatusResponse> list = new ArrayList<>();

        for (; !d.isAfter(end); d = d.plusDays(1)) {
            boolean weekend = isWeekend(d);
            boolean holiday = isHoliday(d);
            boolean dayOff = weekend || holiday;
            String name = holiday ? getActualHolidayName(d).orElse(null) : null;
            list.add(new DayOffStatusResponse(d, weekend, holiday, dayOff, name));
        }

        return list;
    }

    /**
     * 기간 내 공휴일을 FullCalendar 배경 이벤트로 반환합니다.
     *
     * <p>
     * 지정된 기간 내에서 실제 공휴일인 날짜들을 찾아
     * FullCalendar에서 배경 이벤트로 표시할 수 있는 형태로 변환합니다.
     * 각 공휴일은 하루 단위로 처리되며, UI에서 배경색으로 강조 표시됩니다.
     * </p>
     *
     * <p>이벤트 속성:</p>
     * <ul>
     *   <li>display: "background" - 배경 이벤트로 표시</li>
     *   <li>className: "fc-holiday-bg" - CSS 클래스명</li>
     *   <li>allDay: true - 종일 이벤트</li>
     * </ul>
     *
     * @param start 시작 날짜 (포함)
     * @param end   종료 날짜 (이 메서드에서는 포함 처리)
     * @return HolidayEventResponse 리스트 (공휴일인 날짜만)
     * @throws IllegalArgumentException start/end가 null이거나 end가 start보다 이전인 경우
     */
    @Transactional(readOnly = true)
    public List<HolidayEventResponse> getHolidayEvents(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("start/end는 null일 수 없습니다.");
        }
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("end가 start보다 이전일 수 없습니다.");
        }

        List<HolidayEventResponse> events = new ArrayList<>();

        // 포함 범위 내 날짜를 순회하며 공휴일인 날만 이벤트 생성
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            Optional<String> name = getActualHolidayName(d);
            if (name.isPresent()) {
                events.add(HolidayEventResponse.background(d, name.get(), "fc-holiday-bg"));
            }
        }

        log.debug("공휴일 이벤트 생성 완료 - 기간: {}~{}, 이벤트 수: {}", start, end, events.size());
        return events;
    }

    // ===============================
    // 검증 / 유틸
    // ===============================

    /**
     * 동적 최대 허용 연도(현재 연도 + 2)를 반환합니다.
     *
     * <p>테스트 용이성을 위해 {@link Clock}을 사용합니다.</p>
     */
    private int dynamicMaxYear() {
        return Year.now(clock).getValue() + 2;
    }

    /**
     * 연도 유효성(하한/상한) 검증.
     *
     * <p>
     * 하한은 {@link #MIN_YEAR}, 상한은 {@link #dynamicMaxYear()} 입니다.
     * 범위를 벗어나면 {@link IllegalArgumentException}을 던집니다.
     * </p>
     *
     * @param year 검증할 연도
     */
    private void validateYear(int year) {
        int max = dynamicMaxYear();
        if (year < MIN_YEAR || year > max) {
            throw new IllegalArgumentException("연도는 " + MIN_YEAR + "–" + max + " 사이여야 합니다. 입력값: " + year);
        }
    }

    /**
     * 날짜 기준 연도 유효성 검증.
     *
     * @param date 검증할 날짜
     */
    private void validateYear(LocalDate date) {
        validateYear(date.getYear());
    }

    /**
     * 연/월 파라미터 유효성 검증.
     *
     * @param year  연도
     * @param month 월(1~12)
     */
    private void validateYearMonth(int year, int month) {
        if (year < 1900) {
            throw new IllegalArgumentException("year는 1900년 이상이어야 합니다. 입력된 값: " + year);
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("month는 1~12 사이여야 합니다. 입력된 값: " + month);
        }
    }

    // ===============================
    // API 응답 파싱
    // ===============================

    /**
     * JSON 응답을 Holiday 엔터티 목록으로 변환합니다.
     *
     * <p>
     * 한국천문연구원 API 응답 구조는 {@code response.body.items.item} 경로에 특일 데이터가 있으며,
     * 다음과 같은 다양한 응답 형태를 안전하게 처리합니다:
     * </p>
     *
     * <ul>
     *   <li><b>데이터 없음</b>: items.item이 존재하지 않거나 null</li>
     *   <li><b>단일 객체</b>: items.item이 JSON 객체 하나</li>
     *   <li><b>복수 객체</b>: items.item이 JSON 배열</li>
     * </ul>
     *
     * @param response   API의 원본 JSON 응답 (null 가능)
     * @param sourceType 데이터 소스 타입 ({@value #SOURCE_TYPE_REST}: 공휴일 API, {@value #SOURCE_TYPE_NATIONAL}: 국경일 API)
     * @return 변환된 Holiday 엔터티 목록 (변환 실패한 항목은 제외됨)
     */
    private List<Holiday> parseHolidayResponse(JsonNode response, String sourceType) {
        if (response == null) {
            log.debug("API 응답이 null입니다. (sourceType={})", sourceType);
            return Collections.emptyList();
        }

        JsonNode items = response.path("response").path("body").path("items").path("item");

        if (items.isMissingNode() || items.isNull()) {
            log.debug("API 응답에 items.item이 없습니다. (sourceType={})", sourceType);
            return Collections.emptyList();
        }

        List<Holiday> results = new ArrayList<>();

        if (items.isArray()) {
            // 복수 객체인 경우: 배열의 각 요소를 처리
            log.debug("복수 특일 데이터 파싱 시작 - sourceType: {}, 개수: {}", sourceType, items.size());
            for (JsonNode item : items) {
                parseAndAddHoliday(results, item, sourceType);
            }
        } else {
            // 단일 객체인 경우: items 자체가 하나의 특일 데이터
            log.debug("단일 특일 데이터 파싱 시작 - sourceType: {}", sourceType);
            parseAndAddHoliday(results, items, sourceType);  // 🔧 수정: item → items
        }

        log.debug("특일 데이터 파싱 완료 - sourceType: {}, 파싱 성공: {}", sourceType, results.size());
        return results;
    }

    /**
     * 개별 JSON 아이템을 파싱하여 결과 목록에 추가하는 헬퍼 메서드.
     *
     * <p>
     * 파싱에 실패한 아이템은 로그를 남기고 건너뛰며,
     * 전체 처리 과정에는 영향을 주지 않습니다.
     * </p>
     *
     * @param results    파싱된 Holiday를 추가할 목록
     * @param item       파싱할 JSON 아이템
     * @param sourceType 데이터 소스 타입
     */
    private void parseAndAddHoliday(List<Holiday> results, JsonNode item, String sourceType) {
        Holiday holiday = parseHolidayItem(item, sourceType);
        if (holiday != null) {
            results.add(holiday);
        }
        // null인 경우는 parseHolidayItem에서 이미 로깅됨
    }

    /**
     * 개별 JSON 아이템을 Holiday 엔터티로 변환합니다.
     *
     * <p>
     * API에서 제공하는 필드들을 Holiday 엔터티의 속성으로 매핑하며,
     * 변환 과정에서 발생할 수 있는 다양한 예외상황을 안전하게 처리합니다.
     * </p>
     *
     * <p>대체공휴일 감지:</p>
     * <p>dateName에 "대체"라는 문자열이 포함된 경우 isSubstitute를 true로 설정합니다.</p>
     *
     * @param item       개별 특일 정보 JSON 객체
     * @param sourceType 데이터 소스 타입 ({@value #SOURCE_TYPE_REST} 또는 {@value #SOURCE_TYPE_NATIONAL})
     * @return 변환된 Holiday 엔터티, 파싱 실패 시 null 반환
     */
    private Holiday parseHolidayItem(JsonNode item, String sourceType) {
        try {
            // 필수 필드 추출
            String locdate = item.path("locdate").asText();
            String dateName = item.path("dateName").asText();

            if (locdate.isEmpty() || dateName.isEmpty()) {
                log.warn("필수 필드 누락 - locdate: {}, dateName: {}", locdate, dateName);
                return null;
            }

            // 선택적 필드 추출
            String dateKind = item.path("dateKind").asText();
            Integer seq = item.path("seq").isMissingNode() ? null : item.path("seq").asInt();

            // 날짜 파싱
            LocalDate date;
            try {
                date = LocalDate.parse(locdate, LOCDATE_FORMATTER);
            } catch (DateTimeParseException e) {
                log.warn("날짜 형식 오류 - locdate: {}, error: {}", locdate, e.getMessage());
                return null;
            }

            // 외부 ID 생성 (중복 방지용)
            String externalId = (seq == null) ? locdate : (locdate + "#" + seq);

            // 대체공휴일 감지 및 타입 결정
            boolean isSubstitute = dateName.contains("대체");
            Holiday.HolidayType holidayType = isSubstitute
                    ? Holiday.HolidayType.SUBSTITUTE_HOLIDAY
                    : mapHolidayType(dateKind, sourceType);

            // Holiday 엔터티 빌드
            return Holiday.builder()
                    .countryCode(COUNTRY_CODE_KR)
                    .startDate(date)
                    .endDate(date)  // 현재는 모든 공휴일이 하루 단위
                    .nameKo(dateName)
                    .isSubstitute(isSubstitute)
                    .holidayType(holidayType)  // 대체공휴일인 경우 SUBSTITUTE_HOLIDAY로 명시적 설정
                    .externalId(externalId)
                    .externalSource("KASI_API")
                    .build();

        } catch (Exception e) {
            log.warn("공휴일 아이템 파싱 실패 - item: {}, sourceType: {}, error: {}",
                    item.toString(), sourceType, e.getMessage(), e);
            return null;
        }
    }

    // ===============================
    // 동일 날짜 중복 처리 / 우선순위
    // ===============================

    /**
     * 동일 날짜의 공휴일 중복을 제거하고 우선순위가 높은 것만 남깁니다.
     *
     * <p>
     * 삼일절처럼 같은 날짜가 REST API(법정공휴일)와 NATIONAL API(국경일)에
     * 모두 존재할 수 있습니다. 이런 경우 타입별 우선순위에 따라 하나만 선택하여
     * DB 저장 시 중복을 방지하고 데이터 품질을 향상시킵니다.
     * </p>
     *
     * <p>우선순위 (높은 순서):</p>
     * <ol>
     *   <li>SUBSTITUTE_HOLIDAY: 대체공휴일 (가장 구체적이고 실용적)</li>
     *   <li>PUBLIC_HOLIDAY: 법정 공휴일 (실제 휴무일로 중요)</li>
     *   <li>NATIONAL_HOLIDAY: 국경일 (기념일적 성격)</li>
     *   <li>기타: 나머지 타입들 (기념일, 24절기 등)</li>
     * </ol>
     *
     * @param holidays 중복 제거할 공휴일 목록
     * @return 동일 날짜 기준으로 우선순위가 높은 공휴일만 포함된 목록
     */
    private List<Holiday> dedupByDateWithPriority(List<Holiday> holidays) {
        if (holidays == null || holidays.isEmpty()) {
            return holidays;
        }

        Map<LocalDate, Holiday> dateToHolidayMap = new LinkedHashMap<>();

        for (Holiday holiday : holidays) {
            LocalDate date = holiday.getStartDate();
            Holiday existing = dateToHolidayMap.get(date);

            if (existing == null) {
                // 해당 날짜의 첫 번째 공휴일
                dateToHolidayMap.put(date, holiday);
            } else {
                // 기존 공휴일과 우선순위 비교
                int existingPriority = getHolidayTypePriority(existing.getHolidayType());
                int newPriority = getHolidayTypePriority(holiday.getHolidayType());

                if (newPriority < existingPriority) { // 숫자가 낮을수록 높은 우선순위
                    log.debug("동일 날짜 공휴일 우선순위 교체 - date: {}, {} → {}",
                            date, existing.getHolidayType(), holiday.getHolidayType());
                    dateToHolidayMap.put(date, holiday);
                } else {
                    log.trace("동일 날짜 공휴일 우선순위 유지 - date: {}, 유지: {}, 제외: {}",
                            date, existing.getHolidayType(), holiday.getHolidayType());
                }
            }
        }

        List<Holiday> dedupedList = new ArrayList<>(dateToHolidayMap.values());

        if (dedupedList.size() < holidays.size()) {
            log.debug("동일 날짜 중복 제거 완료 - 원본: {}개, 정리 후: {}개",
                    holidays.size(), dedupedList.size());
        }

        return dedupedList;
    }

    /**
     * 공휴일 타입별 우선순위를 반환합니다.
     *
     * <p>
     * 숫자가 낮을수록 높은 우선순위를 의미합니다.
     * 동일한 날짜에 여러 타입의 공휴일이 있을 때 선택 기준으로 사용됩니다.
     * </p>
     *
     * @param type 공휴일 타입
     * @return 우선순위 (0이 최고 우선순위)
     */
    private int getHolidayTypePriority(Holiday.HolidayType type) {
        return switch (type) {
            case SUBSTITUTE_HOLIDAY -> 0;  // 대체공휴일 (최우선)
            case PUBLIC_HOLIDAY -> 1;  // 법정 공휴일
            case NATIONAL_HOLIDAY -> 2;  // 국경일
            case MEMORIAL_DAY -> 3;  // 기념일
            case SEASONAL_DIVISION -> 4;  // 24절기
            case TRADITIONAL_DAY -> 5;  // 전통 기념일
            default -> 6;  // 기타 (최하위)
        };
    }

    // ===============================
    // 저장 / 매핑
    // ===============================

    /**
     * 중복을 제외한 새로운 공휴일만 DB에 저장합니다.
     *
     * <p>
     * 동일한 국가코드, 시작날짜, 종료날짜, 한국어명을 가진 공휴일이 이미 존재하는지 확인하고,
     * 중복되지 않는 공휴일만 배치로 저장합니다. 이를 통해 API 중복 호출이나
     * 동시성 문제로 인한 중복 데이터 생성을 방지합니다.
     * </p>
     *
     * <p>중복 검사 기준:</p>
     * <ul>
     *   <li>countryCode: 국가 코드 (현재는 "KR" 고정)</li>
     *   <li>startDate: 공휴일 시작 날짜</li>
     *   <li>endDate: 공휴일 종료 날짜 (현재는 하루 단위이므로 startDate와 동일)</li>
     *   <li>nameKo: 한국어 공휴일명</li>
     * </ul>
     *
     * @param holidays 저장할 후보 공휴일 목록
     * @param year     로깅용 연도
     * @param month    로깅용 월
     */
    private void saveNewHolidays(List<Holiday> holidays, int year, int month) {
        if (holidays == null || holidays.isEmpty()) {
            log.debug("저장할 공휴일이 없습니다 - year: {}, month: {}", year, month);
            return;
        }

        List<Holiday> holidaysToSave = new ArrayList<>();

        for (Holiday holiday : holidays) {
            boolean exists = holidayRepository.existsByCountryCodeAndStartDateAndEndDateAndNameKo(
                    holiday.getCountryCode(),
                    holiday.getStartDate(),
                    holiday.getEndDate(),
                    holiday.getNameKo()
            );

            if (!exists) {
                holidaysToSave.add(holiday);
            } else {
                log.trace("중복된 공휴일 저장 제외 - {}: {}", holiday.getStartDate(), holiday.getNameKo());
            }
        }

        if (!holidaysToSave.isEmpty()) {
            holidayRepository.saveAll(holidaysToSave);
            log.info("새로운 공휴일 저장 완료 - year: {}, month: {}, 전체: {}, 신규 저장: {}",
                    year, month, holidays.size(), holidaysToSave.size());
        } else {
            log.debug("새로 저장할 공휴일 없음 (모두 중복) - year: {}, month: {}, 전체: {}",
                    year, month, holidays.size());
        }
    }

    /**
     * API의 dateKind 코드를 HolidayType Enum으로 매핑합니다.
     *
     * <p>
     * 한국천문연구원 API에서 제공하는 dateKind 코드를
     * 내부 시스템의 HolidayType으로 변환합니다.
     * 또한 호출한 API 엔드포인트(sourceType)에 따라
     * 동일한 dateKind라도 다르게 분류할 수 있습니다.
     * </p>
     *
     * <p>dateKind 코드 매핑:</p>
     * <ul>
     *   <li><b>01</b>: 국경일/공휴일
     *     <ul>
     *       <li>NATIONAL API → NATIONAL_HOLIDAY (국경일)</li>
     *       <li>REST API → PUBLIC_HOLIDAY (법정 공휴일)</li>
     *     </ul>
     *   </li>
     *   <li><b>02</b>: MEMORIAL_DAY (기념일)</li>
     *   <li><b>03</b>: SEASONAL_DIVISION (24절기)</li>
     *   <li><b>04</b>: TRADITIONAL_DAY (전통 기념일)</li>
     *   <li><b>기타</b>: PUBLIC_HOLIDAY (기본값)</li>
     * </ul>
     *
     * <p>실제 휴무일 판단:</p>
     * <p>{@link #isHoliday(LocalDate)} 메서드에서는 NATIONAL_HOLIDAY, PUBLIC_HOLIDAY,
     * SUBSTITUTE_HOLIDAY만 실제 휴무일로 인정합니다.</p>
     *
     * @param dateKind   API에서 제공하는 날짜 종류 코드 ("01", "02", "03", "04" 등)
     * @param sourceType 호출한 API 엔드포인트 타입 ({@value #SOURCE_TYPE_REST} 또는 {@value #SOURCE_TYPE_NATIONAL})
     * @return 매핑된 HolidayType
     */
    private Holiday.HolidayType mapHolidayType(String dateKind, String sourceType) {
        return switch (dateKind) {
            case "01" -> SOURCE_TYPE_NATIONAL.equals(sourceType)
                    ? Holiday.HolidayType.NATIONAL_HOLIDAY
                    : Holiday.HolidayType.PUBLIC_HOLIDAY;
            case "02" -> Holiday.HolidayType.MEMORIAL_DAY;
            case "03" -> Holiday.HolidayType.SEASONAL_DIVISION;
            case "04" -> Holiday.HolidayType.TRADITIONAL_DAY;
            default -> {
                log.debug("알 수 없는 dateKind: {}, 기본값(PUBLIC_HOLIDAY)으로 처리", dateKind);
                yield Holiday.HolidayType.PUBLIC_HOLIDAY; // 알 수 없는 타입은 기본 공휴일로 처리
            }
        };
    }
}
