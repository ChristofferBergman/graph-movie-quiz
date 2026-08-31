package com.graphragmoviequiz.api.web.model;

import java.util.UUID;

public record GameResponse(
        UUID id,
        String player,
        int score,
        int remainingRag,
        int remainingGraphRag,
        QuestionResponse question
) {
}
