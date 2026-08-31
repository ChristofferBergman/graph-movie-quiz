package com.graphragmoviequiz.api.highscore;

import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class Neo4jHighScoreRepository implements HighScoreRepository {

    private static final String ADOPT_EXISTING_SINGLETON = """
            MATCH (highScore:HighScore)
            WHERE highScore.id IS NULL
            SET highScore.id = 'global'
            """;

    private static final String ENSURE_SINGLETON_CONSTRAINT = """
            CREATE CONSTRAINT high_score_singleton IF NOT EXISTS
            FOR (highScore:HighScore) REQUIRE highScore.id IS UNIQUE
            """;

    private static final String FINISH_GAME = """
            MATCH (game:Game {uuid: $uuid})
            MERGE (highScore:HighScore {id: 'global'})
            SET highScore.lockVersion = coalesce(highScore.lockVersion, 0) + 1
            WITH highScore, game
            OPTIONAL MATCH (highScore)-[:HIGHSCORE]->(existing:Game)
            WITH highScore,
                 game,
                 count(existing) AS existingCount,
                 min(existing.score) AS lowestScore
            WITH highScore,
                 game,
                 existingCount < 3 OR game.score > coalesce(lowestScore, -1) AS qualifies
            FOREACH (_ IN CASE WHEN qualifies THEN [1] ELSE [] END |
                MERGE (highScore)-[:HIGHSCORE]->(game)
            )
            WITH highScore, game, qualifies
            CALL (highScore) {
                MATCH (highScore)-[:HIGHSCORE]->(ranked:Game)
                WITH ranked
                ORDER BY ranked.score DESC, ranked.lastActivity ASC, ranked.uuid ASC
                WITH collect(ranked) AS rankedGames
                UNWIND rankedGames[3..] AS removed
                DETACH DELETE removed
            }
            FOREACH (_ IN CASE WHEN qualifies THEN [] ELSE [1] END |
                DETACH DELETE game
            )
            """;

    private static final String FIND_TOP_THREE = """
            MATCH (:HighScore {id: 'global'})-[:HIGHSCORE]->(game:Game)
            RETURN game.player AS player, game.score AS score
            ORDER BY game.score DESC, game.lastActivity ASC, game.uuid ASC
            LIMIT 3
            """;

    private final Driver driver;
    private final SessionConfig sessionConfig;
    private volatile boolean schemaReady;

    public Neo4jHighScoreRepository(
            Driver driver,
            @Value("${spring.data.neo4j.database}") String database
    ) {
        this.driver = driver;
        this.sessionConfig = SessionConfig.builder().withDatabase(database).build();
    }

    @Override
    public void finishGame(UUID gameId) {
        ensureSchema();
        try (var session = driver.session(sessionConfig)) {
            session.executeWriteWithoutResult(transaction ->
                    transaction.run(FINISH_GAME, Map.of("uuid", gameId.toString())).consume()
            );
        }
    }

    @Override
    public List<HighScoreEntry> findTopThree() {
        ensureSchema();
        try (var session = driver.session(sessionConfig)) {
            return session.executeRead(transaction ->
                    transaction.run(FIND_TOP_THREE).list(record -> new HighScoreEntry(
                            record.get("player").asString(),
                            record.get("score").asInt()
                    ))
            );
        }
    }

    private synchronized void ensureSchema() {
        if (schemaReady) {
            return;
        }
        try (var session = driver.session(sessionConfig)) {
            session.run(ADOPT_EXISTING_SINGLETON).consume();
            session.run(ENSURE_SINGLETON_CONSTRAINT).consume();
        }
        schemaReady = true;
    }
}
