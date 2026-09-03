package com.graphragmoviequiz.api.web.model;

public record QuestionResponse(
        String movie,
        int movieYear,
        String person,
        int personBorn,
        String connectionMovie,
        boolean ragUsed,
        boolean graphRagUsed
) {
}
