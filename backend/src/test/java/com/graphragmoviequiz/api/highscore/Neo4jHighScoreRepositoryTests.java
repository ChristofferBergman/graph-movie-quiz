package com.graphragmoviequiz.api.highscore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionContext;

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Neo4jHighScoreRepositoryTests {

    @Mock
    private Driver driver;

    @Mock
    private Session session;

    @Mock
    private TransactionContext transaction;

    @Mock
    private Result result;

    private Neo4jHighScoreRepository repository;

    @BeforeEach
    void setUp() {
        repository = new Neo4jHighScoreRepository(driver, "neo4j");
        when(driver.session(any(SessionConfig.class))).thenReturn(session);
    }

    @Test
    void serializesQualificationOnConstrainedSingletonAndKeepsThreeGames() {
        when(session.run(anyString())).thenReturn(result);
        when(transaction.run(anyString(), anyMap())).thenReturn(result);
        executeWriteConsumer();

        repository.finishGame(UUID.randomUUID());

        var schemaQuery = ArgumentCaptor.forClass(String.class);
        verify(session, times(2)).run(schemaQuery.capture());
        assertThat(schemaQuery.getAllValues())
                .anyMatch(query -> query.contains("REQUIRE highScore.id IS UNIQUE"));

        var rankingQuery = ArgumentCaptor.forClass(String.class);
        verify(transaction).run(rankingQuery.capture(), anyMap());
        assertThat(rankingQuery.getValue())
                .contains("lockVersion")
                .contains("game.score > coalesce(lowestScore, -1)")
                .contains("rankedGames[3..]");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void executeWriteConsumer() {
        org.mockito.Mockito.doAnswer(invocation -> {
            var consumer = (Consumer<TransactionContext>) invocation.getArgument(0);
            consumer.accept(transaction);
            return null;
        }).when(session).executeWriteWithoutResult(any(Consumer.class));
    }
}
