package com.graphragmoviequiz.api.game;

import java.time.ZonedDateTime;
import java.util.UUID;

public record Game(
        UUID id,
        String player,
        int score,
        int remainingRag,
        int remainingGraphRag,
        ZonedDateTime lastActivity
) {
}
