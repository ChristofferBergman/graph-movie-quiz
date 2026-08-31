The Neo4j database contains (:Person) and (:Movie) nodes. Persons are connected to Movies
with one of [ACTED_IN], [COMPOSED], [DIRECTED], [PRODUCED] or [WRITTEN]

The query used to find the next question is:

```cypher
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

RETURN p1, p2, m1, m2 LIMIT 1
```

The question to form from that query is:
"Who in *m2* starred in another movie with *p1*?"
And the answer is: *p2*
*m1* Is used when the GraphRAG token is played (see product_vision.md)

When showing clues for either *m1* or *m2* use this query:

```cypher
MATCH (m:Movie) WHERE m.id = $clueMovieId
MATCH (p:Person)-[a:ACTED_IN]->(m) WHERE a.order < 6
RETURN p.name
```

The query used to populate possible answers when the user has types at least
two characters should be:

```cypher
MATCH (p:Person) WHERE p.searchName STARTS WITH toLower($typed)
RETURN p.name LIMIT 5
```

Since the backend runs on a Google Cloud Run instane that cannot keep the game state
in RAM we will also use the Neo4j instance for that. When a player starts a new
game, create a (:Game) node with the following properties:
uuid: String
player: String
score: number
remainingRAG: number
remainingGraphRAG: number
lastActivity: datetime
For the current question, add [QUESTION_MOVIE], [QUESTION_PERSON], [ANSWER_PERSON]
and [CLUE_MOVIE] to the nodes for the quesion. Those relationships should be deleted when the
next question is generated.
When a game is over, check if it should be on the high score list (i.e. there are currently
less that three high scores, or we have a higher point that any of the current high scores).
If it should go on the high score list add [:HIGHSCORE] from (:HighScore) to the (:Game)
and detach delete the (:Game) with the lowest score if there are more than 3.
If it did not make the high score list just detach delete it immediately when the game is
over.
lastActivity should be updated whenever an answer is given.

Whenever creating a new game, also cleanup old games that never finished. Use this
query for that:

```cypher
MATCH (g:Game) WHERE duration.inDays(g.lastActivity, datetime()).days > 3
DETACH DELETE g
```

AS mentioned we also need a highscore list. This is represented with a (:HighScore) node
that is connected to the three top (:Game) nodes with [:HIGHSCORE]. When shown in the UI
it sorts them by score descending.

```cypher
MATCH (:HighScore)-->(g:Game)
RETURN g.player AS Player, g.score AS Score ORDER BY g.score DESC LIMIT 3
```

There is only ever one (:HighScore) node. If it doesn't exist, create it when needed.
