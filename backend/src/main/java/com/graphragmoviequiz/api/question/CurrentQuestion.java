package com.graphragmoviequiz.api.question;

public record CurrentQuestion(
        String movie,
        int movieYear,
        String person,
        int personBorn,
        String connectionMovie
) {
}
