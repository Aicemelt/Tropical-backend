package com.tropical.backend.diary.entity;

/**
 * 일기 감정 상태 열거형
 *
 * <p>
 * 사용자가 일기 작성 시 선택할 수 있는 감정 상태를 정의하는 열거형입니다.
 * 5가지 주요 감정 카테고리로 구성되어 있으며, 각 감정은 한글 설명과 함께 제공됩니다.
 * 감정 데이터는 일기 분석, 감정 통계, 그리고 사용자의 정서적 패턴 파악에 활용됩니다.
 * </p>
 *
 * <p>포함된 감정 상태:</p>
 * <ul>
 *   <li>JOY (기쁨) - 긍정적이고 행복한 감정 상태</li>
 *   <li>SADNESS (슬픔) - 우울하거나 슬픈 감정 상태</li>
 *   <li>ANGER (분노) - 화나거나 짜증이 난 감정 상태</li>
 *   <li>CALM (평온) - 차분하고 안정된 감정 상태</li>
 *   <li>ANXIETY (불안) - 걱정되거나 불안한 감정 상태</li>
 * </ul>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.14
 */
public enum Emotion {
    /** 기쁨 - 긍정적이고 행복한 감정 상태 */
    JOY("기쁨"),

    /** 슬픔 - 우울하거나 슬픈 감정 상태 */
    SADNESS("슬픔"),

    /** 분노 - 화나거나 짜증이 난 감정 상태 */
    ANGER("분노"),

    /** 평온 - 차분하고 안정된 감정 상태 */
    CALM("평온"),

    /** 불안 - 걱정되거나 불안한 감정 상태 */
    ANXIETY("불안");

    /** 감정 상태의 한글 설명 */
    private final String description;

    /**
     * 감정 열거형 생성자
     *
     * @param description 감정 상태의 한글 설명
     */
    Emotion(String description) {
        this.description = description;
    }

    /**
     * 감정 상태의 한글 설명을 반환합니다.
     *
     * <p>
     * UI에서 사용자에게 표시되는 한글 설명을 제공하여
     * 더 직관적인 감정 선택이 가능하도록 지원합니다.
     * </p>
     *
     * @return 감정 상태의 한글 설명
     */
    public String getDescription() {
        return description;
    }
}
