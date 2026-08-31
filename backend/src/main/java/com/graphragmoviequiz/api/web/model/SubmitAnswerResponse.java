package com.graphragmoviequiz.api.web.model;

public record SubmitAnswerResponse(
        boolean correct,
        int score,
        GameResponse game
) {
}
