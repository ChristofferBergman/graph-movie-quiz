package com.graphragmoviequiz.api.web.model;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiModelValidationTests {

    private static Validator validator;

    @BeforeAll
    static void createValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsBlankPlayerName() {
        var violations = validator.validate(new CreateGameRequest(" "));

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly("player");
    }

    @Test
    void rejectsPlayerNameLongerThanFiftyCharacters() {
        var violations = validator.validate(new CreateGameRequest("a".repeat(51)));

        assertThat(violations).hasSize(1);
    }

    @Test
    void acceptsValidRequests() {
        assertThat(validator.validate(new CreateGameRequest("Chris"))).isEmpty();
        assertThat(validator.validate(new SubmitAnswerRequest("Diego Luna"))).isEmpty();
        assertThat(validator.validate(new UseTokenRequest(TokenType.GRAPH_RAG))).isEmpty();
    }
}
