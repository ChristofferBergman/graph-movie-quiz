package com.graphragmoviequiz.api.game;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class Neo4jGameRepository implements GameRepository {

    private static final int INITIAL_TOKEN_COUNT = 2;

    private static final String DELETE_EXPIRED_GAMES = """
            MATCH (g:Game)
            WHERE duration.inDays(g.lastActivity, datetime()).days > 3
              AND NOT (:HighScore)-[:HIGHSCORE]->(g)
            DETACH DELETE g
            """;

    private static final String CREATE_GAME = """
            CREATE (g:Game {
                uuid: $uuid,
                player: $player,
                score: 0,
                remainingRAG: $remainingRAG,
                remainingGraphRAG: $remainingGraphRAG,
                ragUsed: false,
                graphRagUsed: false,
                lastActivity: datetime($lastActivity)
            })
            RETURN g.uuid AS uuid,
                   g.player AS player,
                   g.score AS score,
                   g.remainingRAG AS remainingRAG,
                   g.remainingGraphRAG AS remainingGraphRAG,
                   g.ragUsed AS ragUsed,
                   g.graphRagUsed AS graphRagUsed,
                   g.lastActivity AS lastActivity
            """;

    private static final String FIND_GAME = """
            MATCH (g:Game {uuid: $uuid})
            WHERE duration.inDays(g.lastActivity, datetime()).days <= 3
            RETURN g.uuid AS uuid,
                   g.player AS player,
                   g.score AS score,
                   g.remainingRAG AS remainingRAG,
                   g.remainingGraphRAG AS remainingGraphRAG,
                   coalesce(g.ragUsed, false) AS ragUsed,
                   coalesce(g.graphRagUsed, false) AS graphRagUsed,
                   g.lastActivity AS lastActivity
            """;

    private static final String UPDATE_GAME = """
            MATCH (g:Game {uuid: $uuid})
            SET g.score = $score,
                g.remainingRAG = $remainingRAG,
                g.remainingGraphRAG = $remainingGraphRAG,
                g.ragUsed = $ragUsed,
                g.graphRagUsed = $graphRagUsed,
                g.lastActivity = datetime($lastActivity)
            RETURN g.uuid AS uuid,
                   g.player AS player,
                   g.score AS score,
                   g.remainingRAG AS remainingRAG,
                   g.remainingGraphRAG AS remainingGraphRAG,
                   g.ragUsed AS ragUsed,
                   g.graphRagUsed AS graphRagUsed,
                   g.lastActivity AS lastActivity
            """;

    private static final String USE_TOKEN = """
            MATCH (g:Game {uuid: $uuid})
            WITH g, CASE
                WHEN $type = 'RAG' AND coalesce(g.ragUsed, false) THEN 'ALREADY_USED'
                WHEN $type = 'GRAPH_RAG' AND coalesce(g.graphRagUsed, false) THEN 'ALREADY_USED'
                WHEN $type = 'RAG' AND coalesce(g.graphRagUsed, false) THEN 'NOT_ALLOWED'
                WHEN $type = 'RAG' AND g.remainingRAG <= 0 THEN 'NO_TOKENS'
                WHEN $type = 'GRAPH_RAG' AND g.remainingGraphRAG <= 0 THEN 'NO_TOKENS'
                ELSE 'APPLIED'
            END AS status
            SET g.remainingRAG = CASE
                    WHEN status = 'APPLIED' AND $type = 'RAG' THEN g.remainingRAG - 1
                    ELSE g.remainingRAG
                END,
                g.remainingGraphRAG = CASE
                    WHEN status = 'APPLIED' AND $type = 'GRAPH_RAG' THEN g.remainingGraphRAG - 1
                    ELSE g.remainingGraphRAG
                END,
                g.ragUsed = CASE
                    WHEN status = 'APPLIED' AND $type = 'RAG' THEN true
                    ELSE coalesce(g.ragUsed, false)
                END,
                g.graphRagUsed = CASE
                    WHEN status = 'APPLIED' AND $type = 'GRAPH_RAG' THEN true
                    ELSE coalesce(g.graphRagUsed, false)
                END
            RETURN status,
                   g.uuid AS uuid,
                   g.player AS player,
                   g.score AS score,
                   g.remainingRAG AS remainingRAG,
                   g.remainingGraphRAG AS remainingGraphRAG,
                   g.ragUsed AS ragUsed,
                   g.graphRagUsed AS graphRagUsed,
                   g.lastActivity AS lastActivity
            """;

    private static final String DELETE_GAME = """
            MATCH (g:Game {uuid: $uuid})
            DETACH DELETE g
            """;

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public Neo4jGameRepository(
            Driver driver,
            @Value("${spring.data.neo4j.database}") String database
    ) {
        this.driver = driver;
        this.sessionConfig = SessionConfig.builder().withDatabase(database).build();
    }

    @Override
    public Game create(String player) {
        var id = UUID.randomUUID();
        var now = ZonedDateTime.now(ZoneOffset.UTC);

        try (var session = driver.session(sessionConfig)) {
            return session.executeWrite(transaction -> {
                transaction.run(DELETE_EXPIRED_GAMES).consume();
                var result = transaction.run(CREATE_GAME, Map.of(
                        "uuid", id.toString(),
                        "player", player,
                        "remainingRAG", INITIAL_TOKEN_COUNT,
                        "remainingGraphRAG", INITIAL_TOKEN_COUNT,
                        "lastActivity", now
                ));
                return mapGame(result.single());
            });
        }
    }

    @Override
    public Optional<Game> findById(UUID id) {
        try (var session = driver.session(sessionConfig)) {
            return session.executeRead(transaction -> {
                var result = transaction.run(FIND_GAME, Map.of("uuid", id.toString()));
                return result.hasNext() ? Optional.of(mapGame(result.single())) : Optional.empty();
            });
        }
    }

    @Override
    public Optional<Game> update(Game game) {
        try (var session = driver.session(sessionConfig)) {
            return session.executeWrite(transaction -> {
                var result = transaction.run(UPDATE_GAME, Map.of(
                        "uuid", game.id().toString(),
                        "score", game.score(),
                        "remainingRAG", game.remainingRag(),
                        "remainingGraphRAG", game.remainingGraphRag(),
                        "ragUsed", game.ragUsed(),
                        "graphRagUsed", game.graphRagUsed(),
                        "lastActivity", game.lastActivity()
                ));
                return result.hasNext() ? Optional.of(mapGame(result.single())) : Optional.empty();
            });
        }
    }

    @Override
    public Optional<TokenUseResult> useToken(UUID id, HelpTokenType type) {
        try (var session = driver.session(sessionConfig)) {
            return session.executeWrite(transaction -> {
                var result = transaction.run(USE_TOKEN, Map.of(
                        "uuid", id.toString(),
                        "type", type.name()
                ));
                if (!result.hasNext()) {
                    return Optional.empty();
                }
                var record = result.single();
                return Optional.of(new TokenUseResult(
                        TokenUseResult.Status.valueOf(record.get("status").asString()),
                        mapGame(record)
                ));
            });
        }
    }

    @Override
    public void deleteById(UUID id) {
        try (var session = driver.session(sessionConfig)) {
            session.executeWriteWithoutResult(transaction ->
                    transaction.run(DELETE_GAME, Map.of("uuid", id.toString())).consume()
            );
        }
    }

    private Game mapGame(Record record) {
        return new Game(
                UUID.fromString(record.get("uuid").asString()),
                record.get("player").asString(),
                record.get("score").asInt(),
                record.get("remainingRAG").asInt(),
                record.get("remainingGraphRAG").asInt(),
                record.get("ragUsed").asBoolean(),
                record.get("graphRagUsed").asBoolean(),
                record.get("lastActivity").asZonedDateTime()
        );
    }
}
