package com.graphragmoviequiz.api.web.model;

import java.util.UUID;
import java.time.ZonedDateTime;

public record GameResponse(
        UUID id,
        String player,
        int score,
        int remainingRag,
        int remainingGraphRag,
        ZonedDateTime questionDeadline,
        QuestionResponse question
) {
}
