package com.tropical.backend.diary.entity;

/**
 * 일기 날씨 상태 열거형
 *
 * <p>
 * 사용자가 일기 작성 시 선택할 수 있는 날씨 상태를 정의하는 열거형입니다.
 * 5가지 주요 날씨 카테고리로 구성되어 있으며, 각 날씨는 한글 설명과 함께 제공됩니다.
 * 날씨 데이터는 감정과의 상관관계 분석, 계절별 통계, 그리고 사용자의
 * 환경적 요인과 감정 상태의 연관성 파악에 활용됩니다.
 * </p>
 *
 * <p>포함된 날씨 상태:</p>
 * <ul>
 *   <li>SUNNY (맑음) - 화창하고 맑은 날씨</li>
 *   <li>CLOUDY (흐림) - 구름이 많고 흐린 날씨</li>
 *   <li>RAINY (비) - 비가 오는 날씨</li>
 *   <li>SNOWY (눈) - 눈이 오는 날씨</li>
 *   <li>WINDY (바람) - 바람이 강한 날씨</li>
 * </ul>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.14
 */
public enum Weather {
    /** 맑음 - 화창하고 맑은 날씨 */
    SUNNY("맑음"),

    /** 흐림 - 구름이 많고 흐린 날씨 */
    CLOUDY("흐림"),

    /** 비 - 비가 오는 날씨 */
    RAINY("비"),

    /** 눈 - 눈이 오는 날씨 */
    SNOWY("눈"),

    /** 바람 - 바람이 강한 날씨 */
    WINDY("바람");

    /** 날씨 상태의 한글 설명 */
    private final String description;

    /**
     * 날씨 열거형 생성자
     *
     * @param description 날씨 상태의 한글 설명
     */
    Weather(String description) {
        this.description = description;
    }

    /**
     * 날씨 상태의 한글 설명을 반환합니다.
     *
     * <p>
     * UI에서 사용자에게 표시되는 한글 설명을 제공하여
     * 더 직관적인 날씨 선택이 가능하도록 지원합니다.
     * </p>
     *
     * @return 날씨 상태의 한글 설명
     */
    public String getDescription() {
        return description;
    }
}
