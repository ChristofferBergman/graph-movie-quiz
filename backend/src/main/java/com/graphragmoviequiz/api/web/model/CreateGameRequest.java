package com.graphragmoviequiz.api.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGameRequest(
        @NotBlank(message = "Player name is required.")
        @Size(max = 50, message = "Player name must be at most 50 characters.")
        String player
) {
}
