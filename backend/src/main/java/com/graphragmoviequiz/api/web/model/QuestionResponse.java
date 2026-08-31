package com.graphragmoviequiz.api.web.model;

public record QuestionResponse(
        String movie,
        String person,
        String connectionMovie,
        boolean ragUsed,
        boolean graphRagUsed
) {
}
