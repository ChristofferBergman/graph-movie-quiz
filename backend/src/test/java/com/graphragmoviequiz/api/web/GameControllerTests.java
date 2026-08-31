package com.graphragmoviequiz.api.web;

import com.graphragmoviequiz.api.error.GlobalExceptionHandler;
import com.graphragmoviequiz.api.game.GameService;
import com.graphragmoviequiz.api.web.model.GameResponse;
import com.graphragmoviequiz.api.web.model.QuestionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class GameControllerTests {

    @Mock
    private GameService gameService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GameController(gameService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createsGame() throws Exception {
        var id = UUID.randomUUID();
        var game = new GameResponse(
                id,
                "Chris",
                0,
                2,
                2,
                new QuestionResponse("Rogue One", "Robert Duvall", false, false)
        );
        when(gameService.createGame("Chris")).thenReturn(game);

        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"player\":\"Chris\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/games/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.question.movie").value("Rogue One"));
    }

    @Test
    void rejectsBlankPlayerName() throws Exception {
        mockMvc.perform(post("/api/v1/games")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"player\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors.player").value("Player name is required."));
    }
}
