package com.graphragmoviequiz.api.web;

import com.graphragmoviequiz.api.highscore.HighScoreService;
import com.graphragmoviequiz.api.web.model.HighScoreEntryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class HighScoreControllerTests {

    @Mock
    private HighScoreService highScoreService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HighScoreController(highScoreService))
                .build();
    }

    @Test
    void returnsOrderedHighScores() throws Exception {
        when(highScoreService.getHighScores()).thenReturn(List.of(
                new HighScoreEntryResponse("Chris", 8),
                new HighScoreEntryResponse("Jane", 6)
        ));

        mockMvc.perform(get("/api/v1/high-scores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].player").value("Chris"))
                .andExpect(jsonPath("$[0].score").value(8))
                .andExpect(jsonPath("$[1].player").value("Jane"));
    }
}
