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
public class Neo4jQuestionRepository implements QuestionRepository {

    private static final String FIND_NEXT_QUESTION = """
            CYPHER 25
            MATCH (p1:Person)-[a1:ACTED_IN]->(m1:Movie)<-[a2:ACTED_IN]-(p2:Person)-[a3:ACTED_IN]->(m2:Movie)
            WHERE p1 <> p2 AND m1 <> m2 AND a1.order < 6 AND a2.order < 6 AND a3.order < 6
            WITH p1, p2, m1, m2 ORDER BY rand()

            CALL(p1, p2, m1, m2) {
              UNWIND CASE
                WHEN NOT EXISTS {
                  (p1)-[:ACTED_IN]->(:Movie)<-[:ACTED_IN]-(p3:Person)-[:ACTED_IN]->(m2)
                  WHERE p3 <> p1 AND p3 <> p2
                } THEN [0]
                ELSE []
                END AS divisor
              RETURN 1 / divisor AS forceError
            } IN TRANSACTIONS OF 1 ROW ON ERROR BREAK REPORT STATUS AS transactionStatus

            WITH p1, p2, m1, m2, transactionStatus
            WHERE transactionStatus.started = TRUE AND transactionStatus.committed = FALSE

            RETURN elementId(p1) AS p1ElementId,
                   elementId(p2) AS p2ElementId,
                   elementId(m1) AS m1ElementId,
                   elementId(m2) AS m2ElementId,
                   p1.name AS person,
                   m2.title AS movie,
                   m1.title AS connectionMovie
            LIMIT 1
            """;

    private static final String DELETE_CURRENT_QUESTION = """
            MATCH (g:Game {uuid: $uuid})-[relationship]->()
            WHERE type(relationship) IN ['QUESTION_MOVIE', 'QUESTION_PERSON', 'ANSWER_PERSON', 'CLUE_MOVIE']
            DELETE relationship
            """;

    private static final String ATTACH_QUESTION = """
            MATCH (g:Game {uuid: $uuid})
            MATCH (p1:Person) WHERE elementId(p1) = $p1ElementId
            MATCH (p2:Person) WHERE elementId(p2) = $p2ElementId
            MATCH (m1:Movie) WHERE elementId(m1) = $m1ElementId
            MATCH (m2:Movie) WHERE elementId(m2) = $m2ElementId
            SET g.ragUsed = false, g.graphRagUsed = false
            CREATE (g)-[:QUESTION_PERSON]->(p1),
                   (g)-[:ANSWER_PERSON]->(p2),
                   (g)-[:CLUE_MOVIE]->(m1),
                   (g)-[:QUESTION_MOVIE]->(m2)
            """;

    private static final String FIND_CURRENT_QUESTION = """
            MATCH (g:Game {uuid: $uuid})-[:QUESTION_MOVIE]->(movie:Movie)
            MATCH (g)-[:QUESTION_PERSON]->(person:Person)
            MATCH (g)-[:CLUE_MOVIE]->(connectionMovie:Movie)
            RETURN movie.title AS movie,
                   person.name AS person,
                   connectionMovie.title AS connectionMovie
            """;

    private static final String FIND_ANSWER = """
            MATCH (g:Game {uuid: $uuid})-[:ANSWER_PERSON]->(answer:Person)
            RETURN answer.name AS name
            """;

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public Neo4jQuestionRepository(
            Driver driver,
            @Value("${spring.data.neo4j.database}") String database
    ) {
        this.driver = driver;
        this.sessionConfig = SessionConfig.builder().withDatabase(database).build();
    }

    @Override
    public CurrentQuestion replaceForGame(UUID gameId) {
        try (var session = driver.session(sessionConfig)) {
            var candidate = session.run(FIND_NEXT_QUESTION).single();
            var parameters = Map.<String, Object>of(
                    "uuid", gameId.toString(),
                    "p1ElementId", candidate.get("p1ElementId").asString(),
                    "p2ElementId", candidate.get("p2ElementId").asString(),
                    "m1ElementId", candidate.get("m1ElementId").asString(),
                    "m2ElementId", candidate.get("m2ElementId").asString()
            );

            session.executeWriteWithoutResult(transaction -> {
                transaction.run(DELETE_CURRENT_QUESTION, Map.of("uuid", gameId.toString())).consume();
                transaction.run(ATTACH_QUESTION, parameters).consume();
            });

            return mapQuestion(candidate);
        }
    }

    @Override
    public Optional<CurrentQuestion> findForGame(UUID gameId) {
        try (var session = driver.session(sessionConfig)) {
            return session.executeRead(transaction -> {
                var result = transaction.run(
                        FIND_CURRENT_QUESTION,
                        Map.of("uuid", gameId.toString())
                );
                return result.hasNext()
                        ? Optional.of(mapQuestion(result.single()))
                        : Optional.empty();
            });
        }
    }

    @Override
    public Optional<String> findAnswerForGame(UUID gameId) {
        try (var session = driver.session(sessionConfig)) {
            return session.executeRead(transaction -> {
                var result = transaction.run(FIND_ANSWER, Map.of("uuid", gameId.toString()));
                return result.hasNext()
                        ? Optional.of(result.single().get("name").asString())
                        : Optional.empty();
            });
        }
    }

    private CurrentQuestion mapQuestion(Record record) {
        return new CurrentQuestion(
                record.get("movie").asString(),
                record.get("person").asString(),
                record.get("connectionMovie").asString()
        );
    }
}
