package com.graphragmoviequiz.api.game;

public record TokenUseResult(
        Status status,
        Game game
) {
    public enum Status {
        APPLIED,
        ALREADY_USED,
        NOT_ALLOWED,
        NO_TOKENS
    }
}
