package com.graphragmoviequiz.api.web.model;

import jakarta.validation.constraints.NotBlank;

public record SubmitAnswerRequest(
        @NotBlank(message = "Answer name is required.")
        String name
) {
}
