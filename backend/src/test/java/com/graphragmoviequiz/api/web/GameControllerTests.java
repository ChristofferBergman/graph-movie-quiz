package com.graphragmoviequiz.api.web;

import com.graphragmoviequiz.api.error.GlobalExceptionHandler;
import com.graphragmoviequiz.api.game.GameService;
import com.graphragmoviequiz.api.web.model.GameResponse;
import com.graphragmoviequiz.api.web.model.QuestionResponse;
import com.graphragmoviequiz.api.web.model.SubmitAnswerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
                ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(40),
                new QuestionResponse("Rogue One", "Robert Duvall", null, false, false)
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

    @Test
    void closesGame() throws Exception {
        var id = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/games/{gameId}", id))
                .andExpect(status().isNoContent());

        verify(gameService).closeGame(id);
    }

    @Test
    void timesOutGame() throws Exception {
        var id = UUID.randomUUID();
        when(gameService.timeoutGame(id)).thenReturn(
                new SubmitAnswerResponse(false, 2, "Diego Luna", null)
        );

        mockMvc.perform(post("/api/v1/games/{gameId}/timeout", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correct").value(false))
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.correctAnswer").value("Diego Luna"));
    }

    @Test
    void usesHelpToken() throws Exception {
        var id = UUID.randomUUID();
        var game = new GameResponse(
                id,
                "Chris",
                2,
                2,
                1,
                ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(40),
                new QuestionResponse("Rogue One", "Robert Duvall", "Open Range", false, true)
        );
        when(gameService.useToken(id, com.graphragmoviequiz.api.web.model.TokenType.GRAPH_RAG))
                .thenReturn(game);

        mockMvc.perform(post("/api/v1/games/{gameId}/tokens", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"GRAPH_RAG\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingGraphRag").value(1))
                .andExpect(jsonPath("$.question.connectionMovie").value("Open Range"))
                .andExpect(jsonPath("$.question.graphRagUsed").value(true));
    }
}
