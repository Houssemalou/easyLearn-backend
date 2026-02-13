package com.free.easyLearn.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentStatsDTO {
    private String level;
    private BigDecimal hoursLearned;
    private Integer totalSessions;
    private long upcomingSessions;
    private long completedSessions;
    private double overallProgress;
    private SkillsDTO skills;
    private List<RoomSummaryDTO> liveRooms;
    private List<RoomSummaryDTO> upcomingSessionsList;
    private List<EvaluationSummaryDTO> recentEvaluations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillsDTO {
        private int pronunciation;
        private int grammar;
        private int vocabulary;
        private int fluency;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSummaryDTO {
        private String id;
        private String name;
        private String language;
        private String level;
        private String objective;
        private String scheduledAt;
        private Integer duration;
        private String status;
        private String professorName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationSummaryDTO {
        private String id;
        private String language;
        private int overallScore;
        private String assignedLevel;
        private String professorName;
        private String createdAt;
    }
}
