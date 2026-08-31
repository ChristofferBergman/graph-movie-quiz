package com.graphragmoviequiz.api.game;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Values;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Neo4jGameRepositoryTests {

    @Mock
    private Driver driver;

    @Mock
    private Session session;

    @Mock
    private TransactionContext transaction;

    @Mock
    private Result cleanupResult;

    @Mock
    private Result gameResult;

    @Mock
    private Record gameRecord;

    private Neo4jGameRepository repository;

    @BeforeEach
    void setUp() {
        repository = new Neo4jGameRepository(driver, "neo4j");
        when(driver.session(any(SessionConfig.class))).thenReturn(session);
    }

    @Test
    void createsGameWithInitialState() {
        var lastActivity = ZonedDateTime.of(2026, 8, 31, 10, 0, 0, 0, ZoneOffset.UTC);
        configureRecord(UUID.randomUUID(), "Chris", 0, 2, 2, lastActivity);
        when(transaction.run(anyString())).thenReturn(cleanupResult);
        when(transaction.run(anyString(), anyMap())).thenReturn(gameResult);
        when(gameResult.single()).thenReturn(gameRecord);
        executeWriteCallback();

        var game = repository.create("Chris");

        assertThat(game.player()).isEqualTo("Chris");
        assertThat(game.score()).isZero();
        assertThat(game.remainingRag()).isEqualTo(2);
        assertThat(game.remainingGraphRag()).isEqualTo(2);
        verify(cleanupResult).consume();
    }

    @Test
    void loadsExistingGame() {
        var id = UUID.randomUUID();
        var lastActivity = ZonedDateTime.of(2026, 8, 31, 10, 0, 0, 0, ZoneOffset.UTC);
        configureRecord(id, "Chris", 3, 1, 0, lastActivity);
        when(transaction.run(anyString(), anyMap())).thenReturn(gameResult);
        when(gameResult.hasNext()).thenReturn(true);
        when(gameResult.single()).thenReturn(gameRecord);
        executeReadCallback();

        var game = repository.findById(id);

        assertThat(game).contains(new Game(id, "Chris", 3, 1, 0, false, false, lastActivity));
    }

    @Test
    void returnsEmptyWhenGameDoesNotExist() {
        when(transaction.run(anyString(), anyMap())).thenReturn(gameResult);
        when(gameResult.hasNext()).thenReturn(false);
        executeReadCallback();

        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void updatesMutableGameState() {
        var id = UUID.randomUUID();
        var lastActivity = ZonedDateTime.of(2026, 8, 31, 11, 0, 0, 0, ZoneOffset.UTC);
        var updatedGame = new Game(id, "Chris", 4, 1, 0, false, false, lastActivity);
        configureRecord(id, "Chris", 4, 1, 0, lastActivity);
        when(transaction.run(anyString(), anyMap())).thenReturn(gameResult);
        when(gameResult.hasNext()).thenReturn(true);
        when(gameResult.single()).thenReturn(gameRecord);
        executeWriteCallback();

        assertThat(repository.update(updatedGame)).contains(updatedGame);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void executeWriteCallback() {
        when(session.executeWrite(any(TransactionCallback.class))).thenAnswer(invocation -> {
            var callback = (TransactionCallback<?>) invocation.getArgument(0);
            return callback.execute(transaction);
        });
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void executeReadCallback() {
        when(session.executeRead(any(TransactionCallback.class))).thenAnswer(invocation -> {
            var callback = (TransactionCallback<?>) invocation.getArgument(0);
            return callback.execute(transaction);
        });
    }

    private void configureRecord(
            UUID id,
            String player,
            int score,
            int remainingRag,
            int remainingGraphRag,
            ZonedDateTime lastActivity
    ) {
        Map<String, Object> properties = Map.of(
                "uuid", id.toString(),
                "player", player,
                "score", score,
                "remainingRAG", remainingRag,
                "remainingGraphRAG", remainingGraphRag,
                "lastActivity", lastActivity
        );
        properties.forEach((key, value) -> when(gameRecord.get(key)).thenReturn(Values.value(value)));
        when(gameRecord.get("ragUsed")).thenReturn(Values.value(false));
        when(gameRecord.get("graphRagUsed")).thenReturn(Values.value(false));
    }
}
