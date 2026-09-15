package com.graphragmoviequiz.api.actor;

import org.neo4j.driver.Driver;
import org.neo4j.driver.SessionConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Repository
public class Neo4jActorRepository implements ActorRepository {

    private static final String AUTOCOMPLETE_INDEX = "person_name_autocomplete";
    private static final Pattern SEARCH_TERMS = Pattern.compile("\\S+");
    private static final String LUCENE_SPECIAL_CHARACTERS = "+-&|!(){}[]^\"~*?:\\/";

    private static final String FIND_SUGGESTIONS = """
            CALL db.index.fulltext.queryNodes('%s', $query) YIELD node AS person
            RETURN DISTINCT person.name AS name
            ORDER BY toLower(name), name
            LIMIT 5
            """.formatted(AUTOCOMPLETE_INDEX);

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
                            Map.of("query", autocompleteQuery(prefix))
                    ).list(record -> record.get("name").asString())
            );
        }
    }

    static String autocompleteQuery(String input) {
        return SEARCH_TERMS.matcher(input.strip())
                .results()
                .map(match -> escapeLuceneTerm(match.group()))
                .map(term -> term.length() >= 4
                        ? "(" + term + "* OR " + term + "~1)"
                        : term + "*")
                .reduce((left, right) -> left + " AND " + right)
                .orElseThrow(() -> new IllegalArgumentException("Autocomplete query must not be blank."));
    }

    private static String escapeLuceneTerm(String term) {
        var escaped = new StringBuilder(term.length());
        term.codePoints().forEach(character -> {
            if (character < 128 && LUCENE_SPECIAL_CHARACTERS.indexOf(character) >= 0) {
                escaped.append('\\');
            }
            escaped.appendCodePoint(character);
        });
        return escaped.toString();
    }
}
