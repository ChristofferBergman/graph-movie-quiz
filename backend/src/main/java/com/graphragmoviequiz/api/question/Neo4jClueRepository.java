package com.graphragmoviequiz.api.question;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class Neo4jClueRepository implements ClueRepository {

    private static final String FIND_QUESTION_MOVIE_CLUE = """
            MATCH (g:Game {uuid: $uuid})-[:QUESTION_MOVIE]->(movie:Movie)
            WHERE coalesce(g.ragUsed, false) OR coalesce(g.graphRagUsed, false)
            MATCH (actor:Person)-[credit:ACTED_IN]->(movie)
            WHERE credit.order < 6
            WITH movie, actor, credit ORDER BY credit.order
            RETURN movie.title AS movie, collect(actor.name)[..5] AS actors
            """;

    private static final String FIND_CONNECTION_MOVIE_CLUE = """
            MATCH (g:Game {uuid: $uuid})-[:CLUE_MOVIE]->(movie:Movie)
            WHERE coalesce(g.graphRagUsed, false)
            MATCH (actor:Person)-[credit:ACTED_IN]->(movie)
            WHERE credit.order < 6
            WITH movie, actor, credit ORDER BY credit.order
            RETURN movie.title AS movie, collect(actor.name)[..5] AS actors
            """;

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public Neo4jClueRepository(
            Driver driver,
            @Value("${spring.data.neo4j.database}") String database
    ) {
        this.driver = driver;
        this.sessionConfig = SessionConfig.builder().withDatabase(database).build();
    }

    @Override
    public Optional<Clue> findQuestionMovieClue(UUID gameId) {
        return findClue(FIND_QUESTION_MOVIE_CLUE, gameId);
    }

    @Override
    public Optional<Clue> findConnectionMovieClue(UUID gameId) {
        return findClue(FIND_CONNECTION_MOVIE_CLUE, gameId);
    }

    private Optional<Clue> findClue(String query, UUID gameId) {
        try (var session = driver.session(sessionConfig)) {
            return session.executeRead(transaction -> {
                var result = transaction.run(query, Map.of("uuid", gameId.toString()));
                return result.hasNext() ? Optional.of(mapClue(result.single())) : Optional.empty();
            });
        }
    }

    private Clue mapClue(Record record) {
        return new Clue(
                record.get("movie").asString(),
                record.get("actors").asList(value -> value.asString())
        );
    }
}
