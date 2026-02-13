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
public class ProfessorStatsDTO {
    private long totalStudents;
    private long upcomingSessions;
    private long completedSessions;
    private BigDecimal rating;
    private long totalEvaluations;
    private double averageEvaluationScore;
    private List<RoomSummaryDTO> liveRooms;
    private List<RoomSummaryDTO> upcomingSessionsList;
    private List<StudentSummaryDTO> myStudents;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomSummaryDTO {
        private String id;
        private String name;
        private String language;
        private String level;
        private String scheduledAt;
        private Integer duration;
        private Integer maxStudents;
        private long participantCount;
        private String status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummaryDTO {
        private String id;
        private String name;
        private String avatar;
        private String level;
        private String nickname;
    }
}
