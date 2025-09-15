package com.tropical.backend.diary.service;

import com.tropical.backend.diary.entity.Diary;
import com.tropical.backend.diary.repository.DiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

/**
 * 일기 관리 서비스 구현체
 *
 * <p>
 * DiaryService 인터페이스의 구현체로, 일기에 대한 비즈니스 로직을 처리합니다.
 * CRUD 기본 기능과 날짜별 조회, 감정/날씨별 조회 등의 기능을 제공합니다.
 * </p>
 *
 * @author 신동준
 * @version 0.1
 * @since 2025.09.15
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DiaryServiceImpl implements DiaryService {

    private final DiaryRepository diaryRepository;

    /**
     * 새로운 일기를 생성합니다
     */
    @Override
    @Transactional
    public Diary createDiary(Diary diary) {
        log.info("일기 생성 요청: 사용자 ID = {}, 제목 = {}, 날짜 = {}",
                diary.getUser().getId(), diary.getTitle(), diary.getDiaryDate());

        // 중복 날짜 검증
        boolean exists = diaryRepository.existsByUser_IdAndDiaryDate(
                diary.getUser().getId(), diary.getDiaryDate());

        if (exists) {
            log.warn("해당 날짜에 이미 일기가 존재합니다: 사용자 ID = {}, 날짜 = {}",
                    diary.getUser().getId(), diary.getDiaryDate());
            throw new RuntimeException("해당 날짜에 이미 일기가 존재합니다: " + diary.getDiaryDate());
        }

        Diary savedDiary = diaryRepository.save(diary);

        log.info("일기 생성 완료: ID = {}", savedDiary.getId());
        return savedDiary;
    }

    /**
     * ID로 일기를 조회합니다
     */
    @Override
    public Diary getDiaryById(Long diaryId) {
        log.info("일기 조회 요청: ID = {}", diaryId);

        return diaryRepository.findById(diaryId)
                .orElseThrow(() -> {
                    log.error("일기를 찾을 수 없습니다: ID = {}", diaryId);
                    return new RuntimeException("일기를 찾을 수 없습니다: " + diaryId);
                });
    }

    /**
     * 사용자의 모든 일기를 조회합니다
     */
    @Override
    public List<Diary> getDiariesByUserId(Long userId) {
        log.info("사용자 일기 조회 요청: 사용자 ID = {}", userId);

        List<Diary> diaries = diaryRepository.findByUser_IdOrderByDiaryDateDesc(userId);

        log.info("사용자 일기 조회 완료: 사용자 ID = {}, 일기 수 = {}", userId, diaries.size());
        return diaries;
    }

    /**
     * 특정 날짜의 일기를 조회합니다
     */
    @Override
    public Optional<Diary> getDiaryByDate(Long userId, LocalDate date) {
        log.info("날짜별 일기 조회 요청: 사용자 ID = {}, 날짜 = {}", userId, date);

        Optional<Diary> diary = diaryRepository.findByUser_IdAndDiaryDate(userId, date);

        if (diary.isPresent()) {
            log.info("날짜별 일기 조회 완료: 사용자 ID = {}, 날짜 = {}, 일기 ID = {}",
                    userId, date, diary.get().getId());
        } else {
            log.info("해당 날짜에 일기가 존재하지 않음: 사용자 ID = {}, 날짜 = {}", userId, date);
        }

        return diary;
    }

    /**
     * 월별 일기를 조회합니다
     */
    @Override
    public List<Diary> getDiariesByMonth(Long userId, int year, int month) {
        log.info("월별 일기 조회 요청: 사용자 ID = {}, 연도 = {}, 월 = {}", userId, year, month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Diary> diaries = diaryRepository.findByUserIdAndDiaryDateBetween(userId, startDate, endDate);

        log.info("월별 일기 조회 완료: 사용자 ID = {}, {}-{}, 일기 수 = {}",
                userId, year, month, diaries.size());
        return diaries;
    }

    /**
     * 감정별 일기를 조회합니다
     */
    @Override
    public List<Diary> getDiariesByEmotion(Long userId, String emotion) {
        log.info("감정별 일기 조회 요청: 사용자 ID = {}, 감정 = {}", userId, emotion);

        List<Diary> diaries = diaryRepository.findByUser_IdAndEmotionOrderByDiaryDateDesc(userId, emotion);

        log.info("감정별 일기 조회 완료: 사용자 ID = {}, 감정 = {}, 일기 수 = {}",
                userId, emotion, diaries.size());
        return diaries;
    }

    /**
     * 날씨별 일기를 조회합니다
     */
    @Override
    public List<Diary> getDiariesByWeather(Long userId, String weather) {
        log.info("날씨별 일기 조회 요청: 사용자 ID = {}, 날씨 = {}", userId, weather);

        List<Diary> diaries = diaryRepository.findByUser_IdAndWeatherOrderByDiaryDateDesc(userId, weather);

        log.info("날씨별 일기 조회 완료: 사용자 ID = {}, 날씨 = {}, 일기 수 = {}",
                userId, weather, diaries.size());
        return diaries;
    }

    /**
     * 일기 정보를 수정합니다
     */
    @Override
    @Transactional
    public Diary updateDiary(Long diaryId, Diary diary) {
        log.info("일기 수정 요청: ID = {}", diaryId);

        Diary existingDiary = getDiaryById(diaryId);

        // 기존 일기 업데이트 (Builder 패턴 사용)
        Diary.DiaryBuilder updatedBuilder = existingDiary.toBuilder();

        if (diary.getTitle() != null) {
            updatedBuilder.title(diary.getTitle());
        }
        if (diary.getContent() != null) {
            updatedBuilder.content(diary.getContent());
        }
        if (diary.getDiaryDate() != null) {
            // 날짜 변경 시 중복 확인
            if (!existingDiary.getDiaryDate().equals(diary.getDiaryDate())) {
                boolean exists = diaryRepository.existsByUser_IdAndDiaryDate(
                        existingDiary.getUser().getId(), diary.getDiaryDate());
                if (exists) {
                    log.warn("변경하려는 날짜에 이미 일기가 존재합니다: 사용자 ID = {}, 날짜 = {}",
                            existingDiary.getUser().getId(), diary.getDiaryDate());
                    throw new RuntimeException("해당 날짜에 이미 일기가 존재합니다: " + diary.getDiaryDate());
                }
            }
            updatedBuilder.diaryDate(diary.getDiaryDate());
        }
        if (diary.getEmotion() != null) {
            updatedBuilder.emotion(diary.getEmotion());
        }
        if (diary.getWeather() != null) {
            updatedBuilder.weather(diary.getWeather());
        }

        Diary updatedDiary = updatedBuilder.build();
        Diary savedDiary = diaryRepository.save(updatedDiary);

        log.info("일기 수정 완료: ID = {}", savedDiary.getId());
        return savedDiary;
    }

    /**
     * 일기를 삭제합니다
     */
    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        log.info("일기 삭제 요청: ID = {}", diaryId);

        Diary diary = getDiaryById(diaryId);
        diaryRepository.delete(diary);

        log.info("일기 삭제 완료: ID = {}", diaryId);
    }

    /**
     * 특정 날짜에 일기가 존재하는지 확인합니다
     */
    @Override
    public boolean existsByDate(Long userId, LocalDate date) {
        log.info("일기 존재 확인 요청: 사용자 ID = {}, 날짜 = {}", userId, date);

        boolean exists = diaryRepository.existsByUser_IdAndDiaryDate(userId, date);

        log.info("일기 존재 확인 완료: 사용자 ID = {}, 날짜 = {}, 존재 여부 = {}",
                userId, date, exists);
        return exists;
    }
}
