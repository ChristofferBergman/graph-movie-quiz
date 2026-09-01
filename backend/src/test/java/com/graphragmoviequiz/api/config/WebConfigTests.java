package com.graphragmoviequiz.api.config;

import com.graphragmoviequiz.api.game.GameService;
import com.graphragmoviequiz.api.web.GameController;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = WebConfigTests.TestConfiguration.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5173")
@WebAppConfiguration
class WebConfigTests {

    private final WebApplicationContext context;
    private MockMvc mockMvc;

    @Autowired
    WebConfigTests(WebApplicationContext context) {
        this.context = context;
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    void allowsDeletePreflightFromConfiguredFrontend() throws Exception {
        mockMvc.perform(options("/api/v1/games/{gameId}", java.util.UUID.randomUUID())
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "DELETE"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("DELETE")));
    }

    @Configuration
    @EnableWebMvc
    @Import({WebConfig.class, GameController.class})
    static class TestConfiguration {

        @Bean
        GameService gameService() {
            return mock(GameService.class);
        }
    }
}
