package com.graphragmoviequiz.api.question;

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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Neo4jClueRepositoryTests {

    @Mock
    private Driver driver;

    @Mock
    private Session session;

    @Mock
    private TransactionContext transaction;

    @Mock
    private Result result;

    @Mock
    private Record firstCredit;

    @Mock
    private Record duplicateCredit;

    @Mock
    private Record secondCredit;

    private Neo4jClueRepository repository;

    @BeforeEach
    void setUp() {
        repository = new Neo4jClueRepository(driver, "neo4j");
        when(driver.session(any(SessionConfig.class))).thenReturn(session);
    }

    @Test
    void defensivelyRemovesDuplicateActorNamesWhilePreservingOrder() {
        when(transaction.run(anyString(), anyMap())).thenReturn(result);
        when(result.list()).thenReturn(List.of(firstCredit, duplicateCredit, secondCredit));
        when(firstCredit.get("movie")).thenReturn(Values.value("Rogue One"));
        configureActor(firstCredit, "Felicity Jones");
        configureActor(duplicateCredit, "Felicity Jones");
        configureActor(secondCredit, "Diego Luna");
        executeReadCallback();

        var clue = repository.findQuestionMovieClue(UUID.randomUUID());

        assertThat(clue).isPresent();
        assertThat(clue.orElseThrow().actors()).containsExactly("Felicity Jones", "Diego Luna");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void executeReadCallback() {
        when(session.executeRead(any(TransactionCallback.class))).thenAnswer(invocation -> {
            var callback = (TransactionCallback<?>) invocation.getArgument(0);
            return callback.execute(transaction);
        });
    }

    private void configureActor(Record record, String actor) {
        when(record.get("actor")).thenReturn(Values.value(actor));
    }
}
