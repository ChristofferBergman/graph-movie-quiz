package com.graphragmoviequiz.api.question;

import java.util.List;

public record Clue(
        String movie,
        List<String> actors
) {
}
