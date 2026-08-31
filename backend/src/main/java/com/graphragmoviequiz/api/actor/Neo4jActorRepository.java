package com.graphragmoviequiz.api.actor;

import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class Neo4jActorRepository implements ActorRepository {

    private static final String FIND_SUGGESTIONS = """
            MATCH (person:Person)
            WHERE person.searchName STARTS WITH toLower($prefix)
            RETURN DISTINCT person.name AS name
            ORDER BY toLower(name), name
            LIMIT 5
            """;

    private final Driver driver;
    private final SessionConfig sessionConfig;

    public Neo4jActorRepository(
            Driver driver,
            @Value("${spring.data.neo4j.database}") String database
    ) {
        this.driver = driver;
        this.sessionConfig = SessionConfig.builder().withDatabase(database).build();
    }

    @Override
    public List<String> findSuggestions(String prefix) {
        try (var session = driver.session(sessionConfig)) {
            return session.executeRead(transaction -> transaction.run(
                            FIND_SUGGESTIONS,
                            Map.of("prefix", prefix)
                    ).list(record -> record.get("name").asString())
            );
        }
    }
}
