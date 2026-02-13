package com.free.easyLearn.dto.summary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryDTO {
    private UUID id;
    private UUID roomId;
    private String roomName;
    private UUID professorId;
    private String professorName;
    private String summary;
    private List<String> keyTopics;
    private List<String> vocabularyCovered;
    private List<String> grammarPoints;
    private List<String> strengths;
    private List<String> areasToImprove;
    private List<String> recommendations;
    private String nextSessionFocus;
    private Integer overallScore;
    private Integer pronunciationScore;
    private Integer grammarScore;
    private Integer vocabularyScore;
    private Integer fluencyScore;
    private Integer participationScore;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
