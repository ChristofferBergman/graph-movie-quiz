package com.graphragmoviequiz.api.web.model;

import jakarta.validation.constraints.NotNull;

public record UseTokenRequest(
        @NotNull(message = "Token type is required.")
        TokenType type
) {
}
