package com.graphragmoviequiz.api.web.model;

import java.util.List;

public record ClueResponse(
        String movie,
        List<String> actors
) {
}
