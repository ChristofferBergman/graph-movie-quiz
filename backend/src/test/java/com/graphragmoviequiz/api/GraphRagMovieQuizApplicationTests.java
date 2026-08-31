package com.graphragmoviequiz.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "NEO4J_URI=bolt://localhost:7687",
        "NEO4J_USERNAME=neo4j",
        "NEO4J_PASSWORD=test-password"
})
class GraphRagMovieQuizApplicationTests {

    @Test
    void contextLoads() {
    }
}
