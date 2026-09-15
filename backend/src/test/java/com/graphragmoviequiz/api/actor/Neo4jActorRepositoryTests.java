package com.graphragmoviequiz.api.actor;

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
import org.neo4j.driver.TransactionCallback;
import org.neo4j.driver.TransactionContext;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Neo4jActorRepositoryTests {

    @Mock
    private Driver driver;

    @Mock
    private Session session;

    @Mock
    private TransactionContext transaction;

    @Mock
    private Result result;

    private Neo4jActorRepository repository;

    @BeforeEach
    void setUp() {
        repository = new Neo4jActorRepository(driver, "neo4j");
    }

    @Test
    void searchesTheFullTextIndexWithPrefixAndFuzzyTerms() {
        when(driver.session(any(SessionConfig.class))).thenReturn(session);
        when(transaction.run(anyString(), anyMap())).thenReturn(result);
        when(result.list(any())).thenReturn(List.of());
        executeReadCallback();

        repository.findSuggestions("Pene Cruz");

        var query = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        var parameters = ArgumentCaptor.forClass(Map.class);
        verify(transaction).run(query.capture(), parameters.capture());
        assertThat(query.getValue())
                .contains("db.index.fulltext.queryNodes('person_name_autocomplete', $query)")
                .contains("ORDER BY toLower(name), name")
                .contains("LIMIT 5");
        assertThat(parameters.getValue())
                .containsEntry("query", "(Pene* OR Pene~1) AND (Cruz* OR Cruz~1)");
    }

    @Test
    void escapesLuceneSyntaxFromUserInput() {
        assertThat(Neo4jActorRepository.autocompleteQuery("D'Angelo (actor)"))
                .isEqualTo("(D'Angelo* OR D'Angelo~1) AND (\\(actor\\)* OR \\(actor\\)~1)");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void executeReadCallback() {
        when(session.executeRead(any(TransactionCallback.class))).thenAnswer(invocation -> {
            var callback = (TransactionCallback<?>) invocation.getArgument(0);
            return callback.execute(transaction);
        });
    }
}
