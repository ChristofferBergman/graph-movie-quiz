package com.graphragmoviequiz.api.web.model;

public record QuestionResponse(
        String movie,
        String person,
        boolean ragUsed,
        boolean graphRagUsed
) {
}
