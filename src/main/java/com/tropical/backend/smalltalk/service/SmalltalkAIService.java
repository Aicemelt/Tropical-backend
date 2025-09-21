/*
package com.tropical.backend.smalltalk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tropical.backend.auth.entity.User;
import com.tropical.backend.smalltalk.dto.request.TopicGenerateRequest;
import com.tropical.backend.smalltalk.dto.response.AISmallTalkResponse;
import com.tropical.backend.smalltalk.entity.SmalltalkTopic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmalltalkAIService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;

    private final UserActivityService userActivityService;

    private final ObjectMapper objectMapper;

    private static final String SYSTEM_TEMPLATE = """
            ## ROLE & GOAL
              당신은 Tropical 사용자를 위한 스몰토크 주제 추천 AI입니다.
              사용자가 제공한 활동 데이터(SCHEDULE, TODO, DIARY, BUCKET)나 정보가 없으면 일반적 관심사(GENERAL)를 참고하여,
              **사용자가 타인과 자연스럽게 나눌 수 있는 대화형 질문 주제를 추천**해야 합니다.
              대화 주제는 요청한 개수만큼 제공하면 됩니다.\s
        
              ## 입력 데이터 형식
              사용자 데이터는 다음 JSON 형식으로 제공됩니다:
        
              **데이터가 있는 경우:**
              {
                  "totalCount": 5,
                  "activities": {
                      "SCHEDULE": [ ... ],
                      "TODO": [ ... ],
                      "DIARY": [ ... ],
                      "BUCKET": [ ... ]
                  }
              }
        
              **데이터가 없는 경우:**
              {
                  "totalCount": 5,
                  "activities": {
                      "SCHEDULE": [],\s
                      "TODO": [],
                      "DIARY": [],
                      "BUCKET": []
                  }
              }
        
              ## 기본 지침
              1. 상황 파악: 시간대, 요일, 계절 등 고려
              2. 관심사 반영: 취미, 관심 분야, 최근 경험 기반
              3. 데이터 활용: 일정(SCHEDULE), 할일(TODO), 일기(DIARY), 버킷리스트(BUCKET) 활용
              4. 복합 주제 생성: 자연스럽게 연결되는 경우만
              5. 데이터 없는 경우: 보편적 주제로 추천
              6. 편안한 분위기: 부담 없는 가벼운 스몰토크
              7. 대화형 의문문 강조: exampleQuestion은 항상 타인과 나눌 수 있는 의문문 형태
              8. 일반화: 특정 사용자 전용 활동은 타인과 공유할 수 있도록 일반화
              9. 소스 우선순위: sources 배열은 가장 많이 참고한 소스부터 순서대로 나열
        
              ## 주제 카테고리
              - 일상적: 오늘 하루, 영화/드라마, 음식, 날씨, 주말 계획
              - 관심사: 취미, 배우고 있는 것, 음악/책, 여행 경험, 동물
              - 생각거리: 재미있는 사실, 추억, 꿈/목표, 소소한 깨달음, 감사
              - 창의적: "만약에…" 상상 질문, 딜레마/선택 질문, 미래 상상, 아이디어 나누기
              - 복합 예시: 운동+건강식, 학습+여가, 여행+새 경험, 친구+취미 공유, 루틴+성장
        
              ## 응답 형식
              **JSON 배열로 요청된 개수만큼 주제 반환, 다른 텍스트 금지**
        
              [
                  {
                      "topicType": "COMPLEX",
                      "topicContent": "헬스장 운동과 영어 학습 병행",
                      "exampleQuestion": "운동과 공부를 병행할 때 어떤 방법을 사용하시나요?",
                      "sources": [
                          { "sourceType": "SCHEDULE", "sourceId": 123 },
                          { "sourceType": "TODO", "sourceId": 456 }
                      ]
                  },
                  {
                      "topicType": "LIFESTYLE",
                      "topicContent": "카페 방문과 여행 계획",
                      "exampleQuestion": "최근 방문한 카페나 여행에서 추천할 만한 경험이 있나요?",
                      "sources": [
                          { "sourceType": "DIARY", "sourceId": 789 },
                          { "sourceType": "BUCKET", "sourceId": 101 }
                      ]
                  }
              ]
           """;

    public String generateSmallTalk(User user, List<SmalltalkTopic> topics) {

        // 1. db에 저장된 주제를 추출
        List<String> contents = topics.stream()
                .map(topic -> topic.getTopicContent())
                .collect(Collectors.toList());

        // 2. AI 요청
        // 2-1. 주제가 있으면 추가 프롬프트 작성, 가져올 주제 개수를 15개로 지정
        // db에 생성된 주제가 없으면 기본 5개 있으면
        int topicCount = topics.isEmpty() ? 5 : 15;

        // db에 생성된 주제가 있는경우 추가 프롬프트 작성
        String userPrompt = topics.isEmpty() ? "" :
                "\n\n이미 추천된 주제는 다음과 같습니다: " + contents +
                        "\n위 주제와 의미적, 주제적으로 다른 새로운 주제를 제시해주세요. " +
                        "다양한 카테고리(일상, 취미, 창의적 질문, 생각거리 등)에서 고르게 추천하고, 기존 주제와의 중복을 최소화하세요." +
                        "\n각 주제는 독특한 관점(예: 상상력 기반 질문, 최근 트렌드, 문화적 맥락)에서 생성하고, 한국어 사용자에게 자연스럽고 흥미로운 대화 주제로 적합해야 합니다.";

        // 3. AI 주제 추천 요청
        // 3-1. AI 에게 요청할  사용자 활동 dto 생성
        TopicGenerateRequest topicGenerateRequest = userActivityService.makeAIRequestDto(user, topicCount);

        // 3-2. AI 주제 추천 생성
        String raw = getTopic(topicGenerateRequest, userPrompt);
        log.info("AI 주제 생성 완료: {}", raw);
        return raw;

    }

    // db 저장
    // 4. db 저장
    // 4-1. Json 응답 파싱
    List<AISmallTalkResponse> responses;

        try {
        responses = objectMapper.readValue(raw, new TypeReference<List<AISmallTalkResponse>>() {});

    } catch (JsonProcessingException e) {
        log.error("AI 응답 JSON 파싱 실패: {}", raw, e);
        throw new RuntimeException("Json 파싱 실패", e);
    }

        if (responses == null || responses.isEmpty()) return;

    // 4-2. 배치 임베딩
    List<String> topicTexts = responses.stream()
            .map(res -> res.topicContent())
            .collect(Collectors.toList());

        if(topicTexts.isEmpty()) return;

    List<double[]> embeddings = batchEmbed(topicTexts);

    // 4-3. db에 저장된 내용이 없는 경우 (최초 주제 추천일 경우)
        if(topics.isEmpty()) {
        List<SmalltalkTopic> smallTalks = new ArrayList<>();
        for(int i = 0; i < responses.size() && i< embeddings.size(); i++) {
            try {
                List<Double> embeddingList = Arrays.stream(embeddings.get(i))
                        .boxed()
                        .collect(Collectors.toList());
                String embedding = objectMapper.writeValueAsString(embeddingList);

                SmalltalkTopic smalltalk = SmalltalkTopic.toEntity(responses.get(i), user, embedding);
                smallTalks.add(smalltalk);
            } catch (JsonProcessingException e) {
                log.error("임베딩 직렬화 실패", e);
                throw new RuntimeException("임베딩 직렬화 실패");
            }
        }
        smalltalkTopicRepository.saveAll(smallTalks);
        return;
    }

    // 4-4. db에 저장된 내용이 있는 경우 유사도 검사 진행

    // 유사도 판단 기준 점수
    double threshold = topics.size() < 10 ? 0.9 : 0.85;
    int maxSaveCount = 5;

    List<SmalltalkTopic> newSmallTalks = new ArrayList<>();

        for (int i = 0; i < responses.size() && i < embeddings.size() && newSmallTalks.size() < maxSaveCount; i++) {
        AISmallTalkResponse res = responses.get(i);
        double[] newEmbedding = embeddings.get(i);

        // 유사도를 판단하여 중복 여부를 확인하는 플래그
        boolean isSimilar = false;

        for (SmalltalkTopic topic : topics) {
            try {

                // db에 저장된 주제의 embedding 가져오기
                List<Double> storedList = objectMapper.readValue(topic.getEmbedding(), new TypeReference<List<Double>>() {
                });
                double[] storedEmbedding = storedList.stream().mapToDouble(Double::doubleValue).toArray();

                // 가져온 embedding 정규화
                double[] normalizeEmbedding = l2normalize(storedEmbedding);

                // 유사도 검사
                double sim = cosineSimilarity(newEmbedding, normalizeEmbedding);
                log.info("유사도 검사 실행");

                if(sim >= threshold) {
                    log.info("유사도 스킵 - userId={} sim={} 기존토픽='{}' 새토픽='{}'",
                            user.getId(), String.format("%.3f", sim), topic.getTopicContent(), res.topicContent());
                    isSimilar = true;
                    log.info("유사한 주제 발견 - 저장 안함: {} (유사도: {})", responses.get(i).topicContent(), sim);
                    break;
                }

            } catch (JsonProcessingException e) {
                log.error("임베딩 직렬화 실패", e);
                throw new RuntimeException("임베딩 직렬화 실패");
            }
        }

        // 유사도 검사를 통과한 응답을 저장
        if(!isSimilar) {
            try {

                List<Double> embeddingList = Arrays.stream(embeddings.get(i))
                        .boxed()
                        .collect(Collectors.toList());
                String embedding = objectMapper.writeValueAsString(embeddingList);

                SmalltalkTopic smalltalk = SmalltalkTopic.toEntity(responses.get(i), user, embedding);
                newSmallTalks.add(smalltalk);

                log.info("유사도 검사 통과: {}", smalltalk);
            } catch (JsonProcessingException e) {
                log.error("임베딩 직렬화 실패", e);
                throw new RuntimeException("임베딩 직렬화 실패");
            }
        }
    }

        if (!newSmallTalks.isEmpty()) {
        smalltalkTopicRepository.saveAll(newSmallTalks);
    }


    */
/**
     * AI 에게 스몰토크 주제 생성을 요청하는 메소드 입니다.
     * @param req
     * @param prompt
     * @return
     *//*

    private String getTopic(TopicGenerateRequest req, String prompt) {

        try {
            String response = chatClient
                    .prompt()
                    .system(SYSTEM_TEMPLATE)
                    .user(u -> u.text(req.toString() + prompt))
                    .call()
                    .content();

            return response;

        } catch (Exception e) {
            log.error("주제 추천 호출 실패: {}", e.getMessage(), e);
            throw new RuntimeException("주제 추천 중 오류 발생: " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    */
/**
     * db에 저장된 AI 추천 주제와 새롭게 생성된 주제의 유사도를 검사하는 메소드입니다.
     * @param vectorA
     * @param vectorB
     * @return
     *//*

    private double cosineSimilarity(double[] vectorA, double[] vectorB) {
        if (vectorA == null || vectorB == null) {
            throw new IllegalArgumentException("Vectors cannot be null");
        }
        if (vectorA.length != vectorB.length) {
            throw new IllegalArgumentException("Vectors must have the same length");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        double denominator = Math.sqrt(normA) * Math.sqrt(normB);
        if (denominator == 0.0) {
            return 0.0; // 제로 벡터 처리
        }
        return dotProduct / denominator;
    }

    */
/**
     * AI 가 생성한 주제의 백터값을 AI Embedding 모델 api 에게 요청하는 메소드입니다.
     * @param texts
     * @return
     *//*

    private List<double[]> batchEmbed(List<String> texts) {
        if (texts == null || texts.isEmpty()) return List.of();

        List<double[]> out = new ArrayList<>(texts.size());
        int batchSize = 64; // 현재 5~15개라 1회 호출로 충분

        for (int i = 0; i < texts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, texts.size());
            List<String> slice = texts.subList(i, end);

            EmbeddingResponse res = embeddingModel.embedForResponse(slice);
            List<Embedding> results = res.getResults(); // 입력 순서대로 반환

            int m = Math.min(results.size(), slice.size());
            for (int r = 0; r < m; r++) {
                double[] v = toArray(results.get(r));   // List<Double> → double[]
                out.add(l2normalize(v));                // L2 정규화(코사인 안정화)
            }
        }
        return out;
    }

    */
/**
     * ai가 반환하는 Embedding 백터 값들을 double 로 매핑하는 메소드입니다.
     * @param e
     * @return
     *//*

    private double[] toArray(Embedding e) {
        float[] floats = e.getOutput();  // api 가 float[] 반환
        double[] arr = new double[floats.length];
        for (int i = 0; i < floats.length; i++) {
            arr[i] = floats[i];  // float → double 변환
        }
        return arr;
    }

    */
/**
     * 코사인 유사도 검사 진행을 위한 L2 정규화 메소드입니다.
     * @param v
     * @return
     *//*

    private double[] l2normalize(double[] v) {
        double sum = 0.0;
        for (double x : v) sum += x * x;
        double norm = Math.sqrt(sum);
        if (norm == 0) return v;

        double[] normalized = new double[v.length];
        for (int i = 0; i < v.length; i++) {
            normalized[i] = v[i] / norm;
        }
        return normalized;
    }
}
*/
