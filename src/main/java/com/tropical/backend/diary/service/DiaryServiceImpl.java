package com.tropical.backend.diary.service;

import com.tropical.backend.auth.entity.User;
import com.tropical.backend.auth.service.UserService;
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
    private final UserService userService;

    /**
     * User ID와 함께 새로운 일기를 생성합니다
     */
    @Override
    @Transactional
    public Diary createDiaryWithUserId(Long userId, Diary diary) {
        log.info("일기 생성 요청: 사용자 ID = {}, 제목 = {}, 날짜 = {}",
                userId, diary.getTitle(), diary.getDiaryDate());

        // User 엔티티 조회 (기존 UserService 메서드 사용)
        User user = userService.findActiveUserById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        // 중복 날짜 검증
        boolean exists = diaryRepository.existsByUser_IdAndDiaryDate(userId, diary.getDiaryDate());
        if (exists) {
            log.warn("해당 날짜에 이미 일기가 존재합니다: 사용자 ID = {}, 날짜 = {}",
                    userId, diary.getDiaryDate());
            throw new RuntimeException("해당 날짜에 이미 일기가 존재합니다: " + diary.getDiaryDate());
        }

        // Diary에 User 설정
        Diary diaryWithUser = diary.toBuilder()
                .user(user)
                .build();

        Diary savedDiary = diaryRepository.save(diaryWithUser);

        log.info("일기 생성 완료: ID = {}", savedDiary.getId());
        return savedDiary;
    }

    /**
     * 일기 ID와 사용자 ID로 일기를 조회합니다 (권한 검증 포함)
     */
    @Override
    public Diary getDiaryByIdAndUserId(Long diaryId, Long userId) {
        log.info("일기 조회 요청: 일기 ID = {}, 사용자 ID = {}", diaryId, userId);

        Diary diary = diaryRepository.findById(diaryId)
                .orElseThrow(() -> {
                    log.error("일기를 찾을 수 없습니다: ID = {}", diaryId);
                    return new RuntimeException("일기를 찾을 수 없습니다: " + diaryId);
                });

        // 사용자 권한 검증
        if (!diary.getUser().getId().equals(userId)) {
            log.warn("일기 조회 권한 없음: 일기 ID = {}, 요청 사용자 ID = {}, 소유자 ID = {}",
                    diaryId, userId, diary.getUser().getId());
            throw new IllegalArgumentException("해당 일기에 접근할 권한이 없습니다");
        }

        return diary;
    }

    /**
     * 사용자 권한을 검증하여 일기를 수정합니다
     */
    @Override
    @Transactional
    public Diary updateDiaryByUserId(Long diaryId, Long userId, Diary diary) {
        log.info("일기 수정 요청: 일기 ID = {}, 사용자 ID = {}", diaryId, userId);

        Diary existingDiary = getDiaryByIdAndUserId(diaryId, userId);

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
                boolean exists = diaryRepository.existsByUser_IdAndDiaryDate(userId, diary.getDiaryDate());
                if (exists) {
                    log.warn("변경하려는 날짜에 이미 일기가 존재합니다: 사용자 ID = {}, 날짜 = {}",
                            userId, diary.getDiaryDate());
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
     * 사용자 권한을 검증하여 일기를 삭제합니다
     */
    @Override
    @Transactional
    public void deleteDiaryByUserId(Long diaryId, Long userId) {
        log.info("일기 삭제 요청: 일기 ID = {}, 사용자 ID = {}", diaryId, userId);

        Diary diary = getDiaryByIdAndUserId(diaryId, userId);
        diaryRepository.delete(diary);

        log.info("일기 삭제 완료: ID = {}", diaryId);
    }

    // === 조회용 메서드들 ===

    @Override
    public List<Diary> getDiariesByUserId(Long userId) {
        log.info("사용자 일기 조회 요청: 사용자 ID = {}", userId);
        List<Diary> diaries = diaryRepository.findByUser_IdOrderByDiaryDateDesc(userId);
        log.info("사용자 일기 조회 완료: 사용자 ID = {}, 일기 수 = {}", userId, diaries.size());
        return diaries;
    }

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

    @Override
    public List<Diary> getDiariesByMonth(Long userId, int year, int month) {
        log.info("월별 일기 조회 요청: 사용자 ID = {}, 연도 = {}, 월 = {}", userId, year, month);
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        List<Diary> diaries = diaryRepository.findByUserIdAndDiaryDateBetween(userId, startDate, endDate);
        log.info("월별 일기 조회 완료: 사용자 ID = {}, {}-{}, 일기 수 = {}", userId, year, month, diaries.size());
        return diaries;
    }

    @Override
    public List<Diary> getDiariesByEmotion(Long userId, String emotion) {
        log.info("감정별 일기 조회 요청: 사용자 ID = {}, 감정 = {}", userId, emotion);
        List<Diary> diaries = diaryRepository.findByUser_IdAndEmotionOrderByDiaryDateDesc(userId, emotion);
        log.info("감정별 일기 조회 완료: 사용자 ID = {}, 감정 = {}, 일기 수 = {}", userId, emotion, diaries.size());
        return diaries;
    }

    @Override
    public List<Diary> getDiariesByWeather(Long userId, String weather) {
        log.info("날씨별 일기 조회 요청: 사용자 ID = {}, 날씨 = {}", userId, weather);
        List<Diary> diaries = diaryRepository.findByUser_IdAndWeatherOrderByDiaryDateDesc(userId, weather);
        log.info("날씨별 일기 조회 완료: 사용자 ID = {}, 날씨 = {}, 일기 수 = {}", userId, weather, diaries.size());
        return diaries;
    }

    @Override
    public boolean existsByDate(Long userId, LocalDate date) {
        log.info("일기 존재 확인 요청: 사용자 ID = {}, 날짜 = {}", userId, date);
        boolean exists = diaryRepository.existsByUser_IdAndDiaryDate(userId, date);
        log.info("일기 존재 확인 완료: 사용자 ID = {}, 날짜 = {}, 존재 여부 = {}", userId, date, exists);
        return exists;
    }

    // === 기존 메서드들 (호환성 유지용 - 사용 안함) ===

    @Override
    @Transactional
    public Diary createDiary(Diary diary) {
        throw new UnsupportedOperationException("createDiaryWithUserId를 사용하세요");
    }

    @Override
    public Diary getDiaryById(Long diaryId) {
        throw new UnsupportedOperationException("getDiaryByIdAndUserId를 사용하세요");
    }

    @Override
    @Transactional
    public Diary updateDiary(Long diaryId, Diary diary) {
        throw new UnsupportedOperationException("updateDiaryByUserId를 사용하세요");
    }

    @Override
    @Transactional
    public void deleteDiary(Long diaryId) {
        throw new UnsupportedOperationException("deleteDiaryByUserId를 사용하세요");
    }
}
