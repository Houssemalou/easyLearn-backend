package com.free.easyLearn.dto.stats;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsDTO {
    private long totalStudents;
    private long totalProfessors;
    private long activeRooms;
    private long scheduledSessions;
    private long completedSessions;
    private long totalEvaluations;
    private double averageEvaluationScore;
    private List<RoomSummaryDTO> liveRooms;
    private List<RoomSummaryDTO> upcomingSessions;
    private List<StudentSummaryDTO> recentStudents;
    private List<LevelDistributionDTO> levelDistribution;

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
        private String professorName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummaryDTO {
        private String id;
        private String name;
        private String email;
        private String avatar;
        private String level;
        private Integer totalSessions;
        private double averageSkill;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelDistributionDTO {
        private String level;
        private long count;
    }
}
