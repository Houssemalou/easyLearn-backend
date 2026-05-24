package com.free.easyLearn.service;

import com.free.easyLearn.dto.ai.AiGenerateRequest;
import com.free.easyLearn.dto.ai.AiGeneratedChallengeDTO;
import com.free.easyLearn.dto.ai.AiGenerateResponse;
import com.free.easyLearn.entity.Professor;
import com.free.easyLearn.exception.BadRequestException;
import com.free.easyLearn.repository.ProfessorRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AiGenerationService {

    private static final int RPM_LIMIT = 15;
    private static final int RPD_LIMIT = 1500;

    private final RestTemplate restTemplate;
    private final ProfessorRepository professorRepository;

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Value("${gemini.model:gemma-4-26b-a4b-it}")
    private String geminiModel;

    private final Cache<Long, Integer> rpmCache;
    private final Cache<String, Integer> rpdCache;

    private final Map<UUID, LocalDate> professorDailyGenerations = new ConcurrentHashMap<>();

    public AiGenerationService(RestTemplate restTemplate, ProfessorRepository professorRepository) {
        this.restTemplate = restTemplate;
        this.professorRepository = professorRepository;

        this.rpmCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();

        this.rpdCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.DAYS)
                .build();
    }

    public AiGenerateResponse generateChallenges(UUID professorUserId, AiGenerateRequest request) {
        Professor professor = professorRepository.findByUserId(professorUserId)
                .orElseThrow(() -> new BadRequestException("Professor not found"));

        checkProfessorDailyLimit(professor.getId());
        checkGlobalRateLimits();

        String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/"
                + geminiModel + ":generateContent?key=" + geminiApiKey;

        String prompt = buildPrompt(request);

        Map<String, Object> requestBody = new HashMap<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, Object>> parts = new ArrayList<>();

        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);

        Map<String, Object> inlineDataPart = new HashMap<>();
        Map<String, String> inlineData = new HashMap<>();
        inlineData.put("mimeType", request.getMimeType());
        inlineData.put("data", request.getFileBase64());
        inlineDataPart.put("inlineData", inlineData);
        parts.add(inlineDataPart);

        content.put("parts", parts);
        requestBody.put("contents", List.of(content));

        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("responseMimeType", "application/json");
        requestBody.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    geminiUrl, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new BadRequestException("Erreur API Gemini: " + response.getStatusCode());
            }

            Map<String, Object> responseBody = response.getBody();
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) responseBody.get("candidates");
            if (candidates == null || candidates.isEmpty()) {
                throw new BadRequestException("Gemini n'a pas retourné de contenu");
            }

            Map<String, Object> firstCandidate = candidates.get(0);
            Map<String, Object> contentResponse = (Map<String, Object>) firstCandidate.get("content");
            if (contentResponse == null) {
                throw new BadRequestException("Gemini n'a pas retourné de contenu");
            }

            List<Map<String, Object>> partsResponse = (List<Map<String, Object>>) contentResponse.get("parts");
            if (partsResponse == null || partsResponse.isEmpty()) {
                throw new BadRequestException("Gemini n'a pas retourné de contenu");
            }

            String text = partsResponse.stream()
                    .filter(p -> !Boolean.TRUE.equals(p.get("thought")))
                    .map(p -> (String) p.get("text"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining());

            if (text.isBlank()) {
                throw new BadRequestException("Gemini n'a pas retourné de contenu");
            }

            incrementCounters(professor.getId());

            List<AiGeneratedChallengeDTO> challenges = parseResponse(text, request);
            return AiGenerateResponse.builder().challenges(challenges).build();

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            String message = e.getMessage();
            if (message != null && (message.contains("429") || message.contains("quota") || message.contains("rate limit"))) {
                throw new BadRequestException("Vous avez terminé votre quota. Revenez ultérieurement.");
            }
            throw new BadRequestException("Erreur lors de la génération: " + e.getMessage());
        }
    }

    private void checkProfessorDailyLimit(UUID professorId) {
        LocalDate today = LocalDate.now();
        LocalDate lastGeneration = professorDailyGenerations.get(professorId);
        if (lastGeneration != null && lastGeneration.equals(today)) {
            throw new BadRequestException("Vous avez terminé votre quota quotidien. Revenez ultérieurement.");
        }
    }

    private void checkGlobalRateLimits() {
        long now = System.currentTimeMillis();
        long oneMinuteAgo = now - 60000;

        int rpmCount = 0;
        for (Map.Entry<Long, Integer> entry : rpmCache.asMap().entrySet()) {
            if (entry.getKey() >= oneMinuteAgo) {
                rpmCount += entry.getValue();
            }
        }

        if (rpmCount >= RPM_LIMIT) {
            throw new BadRequestException("Trop de requêtes. Veuillez patienter une minute avant de réessayer.");
        }

        String today = LocalDate.now().toString();
        Integer rpdCount = rpdCache.getIfPresent(today);
        if (rpdCount != null && rpdCount >= RPD_LIMIT) {
            throw new BadRequestException("Vous avez terminé votre quota quotidien. Revenez ultérieurement.");
        }
    }

    private void incrementCounters(UUID professorId) {
        long now = System.currentTimeMillis();
        rpmCache.put(now, 1);

        String today = LocalDate.now().toString();
        rpdCache.asMap().merge(today, 1, Integer::sum);

        professorDailyGenerations.put(professorId, LocalDate.now());
    }

    @SuppressWarnings("unchecked")
    private List<AiGeneratedChallengeDTO> parseResponse(String text, AiGenerateRequest request) {
        try {
            if (text.startsWith("```")) {
                text = text.replaceAll("```(?:json)?", "").trim();
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Map<String, Object>> items = mapper.readValue(text, List.class);

            List<AiGeneratedChallengeDTO> challenges = new ArrayList<>();
            String defaultSubject = mapSubjectBack(request.getSubject());

            for (Map<String, Object> item : items) {
                if (item == null) continue;

                String subject = item.containsKey("subject") && isValidSubject((String) item.get("subject"))
                        ? (String) item.get("subject") : defaultSubject;
                String difficulty = item.containsKey("difficulty") && isValidDifficulty((String) item.get("difficulty"))
                        ? (String) item.get("difficulty") : "medium";
                String title = item.get("title") != null ? (String) item.get("title") : "";
                String question = item.get("question") != null ? (String) item.get("question") : "";
                List<String> options = item.get("options") instanceof List
                        ? ((List<Object>) item.get("options")).stream().map(String::valueOf).collect(Collectors.toList())
                        : List.of("", "", "", "");
                int correctAnswer = item.get("correctAnswer") instanceof Number
                        ? ((Number) item.get("correctAnswer")).intValue() : 0;
                int basePoints = item.get("basePoints") instanceof Number
                        ? ((Number) item.get("basePoints")).intValue() : 50;

                if (correctAnswer < 0) correctAnswer = 0;
                if (correctAnswer > 3) correctAnswer = 3;
                if (basePoints < 10) basePoints = 10;
                if (basePoints > 200) basePoints = 200;
                if (options.size() != 4) options = List.of("", "", "", "");

                if (!title.isBlank() && !question.isBlank() && options.stream().allMatch(o -> !o.isBlank())) {
                    challenges.add(AiGeneratedChallengeDTO.builder()
                            .subject(subject)
                            .difficulty(difficulty)
                            .title(title)
                            .question(question)
                            .options(options)
                            .correctAnswer(correctAnswer)
                            .basePoints(basePoints)
                            .targetLevel(request.getLevel())
                            .expiresIn(168)
                            .build());
                }
            }

            if (challenges.isEmpty()) {
                throw new BadRequestException("Gemini n'a pas généré de défis valides. Essayez avec un autre fichier.");
            }

            return challenges.stream().limit(request.getCount()).collect(Collectors.toList());

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Réponse Gemini invalide (JSON mal formé)");
        }
    }

    private String buildPrompt(AiGenerateRequest request) {
        String subjectName = getSubjectFrenchName(request.getSubject());
        String challengeSubject = mapSubjectBack(request.getSubject());

        return "Tu es un professeur expert. À partir du cours ci-joint, génère EXACTEMENT "
                + request.getCount() + " défis (QCM) au format JSON.\n\n"
                + "Contraintes:\n"
                + "- Matière du cours: " + subjectName + "\n"
                + "- Matière pour les défis: \"" + challengeSubject + "\" (parmi: Mathematics, Physics, Chemistry, Biology, EarthScience, French, English, Arabic)\n"
                + "- Niveau scolaire: " + request.getLevel() + "\n"
                + "- Chaque défi doit avoir EXACTEMENT 4 options\n"
                + "- La réponse correcte (correctAnswer) est l'index 0-3\n"
                + "- basePoints: entre 10 et 200\n"
                + "- difficulty: \"easy\", \"medium\", ou \"hard\"\n"
                + "- expiresIn: 168 (heures, soit 7 jours)\n"
                + "- targetLevel: \"" + request.getLevel() + "\" (même niveau que le cours)\n"
                + "- Titre et question en français\n\n"
                + "Formattage IMPORTANT:\n"
                + "- Génère UNIQUEMENT un tableau JSON valide\n"
                + "- Format: [{ \"subject\": \"" + challengeSubject + "\", \"difficulty\": \"easy|medium|hard\", \"title\": \"...\", \"question\": \"...\", \"options\": [\"A\",\"B\",\"C\",\"D\"], \"correctAnswer\": 0-3, \"basePoints\": 50, \"targetLevel\": \"" + request.getLevel() + "\", \"expiresIn\": 168 }]\n"
                + "- Rien d'autre que le tableau JSON. Pas de markdown, pas de ```json.\n"
                + "- Chaque défi doit être pertinent par rapport au contenu du cours fourni.";
    }

    private String getSubjectFrenchName(String subject) {
        Map<String, String> map = Map.ofEntries(
                Map.entry("ARABIC", "Arabe"),
                Map.entry("FRENCH", "Français"),
                Map.entry("ENGLISH", "Anglais"),
                Map.entry("MATHEMATICS", "Mathématiques"),
                Map.entry("SCIENCE", "Sciences"),
                Map.entry("HISTORY_GEOGRAPHY", "Histoire-Géographie"),
                Map.entry("CIVIC_EDUCATION", "Éducation civique"),
                Map.entry("ISLAMIC_EDUCATION", "Éducation islamique"),
                Map.entry("TECHNOLOGY", "Technologie"),
                Map.entry("ARTS", "Arts"),
                Map.entry("OTHER", "Autre")
        );
        return map.getOrDefault(subject, subject);
    }

    private String mapSubjectBack(String subject) {
        Map<String, String> map = Map.of(
                "ARABIC", "Arabic",
                "FRENCH", "French",
                "ENGLISH", "English",
                "MATHEMATICS", "Mathematics"
        );
        return map.getOrDefault(subject, "Mathematics");
    }

    private boolean isValidSubject(String subject) {
        return List.of("Mathematics", "Physics", "Chemistry", "Biology", "EarthScience",
                "French", "English", "Arabic").contains(subject);
    }

    private boolean isValidDifficulty(String difficulty) {
        return List.of("easy", "medium", "hard").contains(difficulty);
    }
}
