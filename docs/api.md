# REST API contract

All endpoints are rooted at `/api/v1`. Request and response bodies use JSON.
UUID values are represented as strings. Errors use `application/problem+json`.

## Games

### Create a game

`POST /api/v1/games`

Request:

```json
{
  "player": "Chris"
}
```

Response: `201 Created` with a `Location` header pointing to the new game.

```json
{
  "id": "b9454d2d-23cc-43ea-a043-dfa05cba079a",
  "player": "Chris",
  "score": 0,
  "remainingRag": 2,
  "remainingGraphRag": 2,
  "question": {
    "movie": "Rogue One",
    "person": "Robert Duvall",
    "ragUsed": false,
    "graphRagUsed": false
  }
}
```

The player name is required and may contain at most 50 characters.

### Load a game

`GET /api/v1/games/{gameId}`

Response: `200 OK` with the same game representation returned when a game is
created. This is used to restore a game after a browser refresh.

Response: `404 Not Found` when the game does not exist or has expired.

### Submit an answer

`POST /api/v1/games/{gameId}/answers`

Request:

```json
{
  "name": "Diego Luna"
}
```

The comparison is case-insensitive but otherwise requires the complete actor
name. The server owns answer validation and score changes.

For a correct answer, the response is `200 OK` and `game` contains the next
question:

```json
{
  "correct": true,
  "score": 4,
  "game": {
    "id": "b9454d2d-23cc-43ea-a043-dfa05cba079a",
    "player": "Chris",
    "score": 4,
    "remainingRag": 1,
    "remainingGraphRag": 2,
    "question": {
      "movie": "Arrival",
      "person": "Jeremy Renner",
      "ragUsed": false,
      "graphRagUsed": false
    }
  }
}
```

For an incorrect answer, `correct` is `false`, `score` is the final score, and
`game` is `null` because the game is over.

## Actor suggestions

`GET /api/v1/actors/suggestions?prefix={typedText}`

The prefix must contain at least two characters. The response is `200 OK` with
at most five alphabetically ordered matches:

```json
[
  { "name": "Diego Luna" },
  { "name": "Diego Tinoco" }
]
```

## Help tokens and clues

### Use a token

`POST /api/v1/games/{gameId}/tokens`

Request:

```json
{
  "type": "GRAPH_RAG"
}
```

The supported values are `RAG` and `GRAPH_RAG`. The response is `200 OK` with
the updated game representation. Repeating the same request for the current
question is idempotent and must not consume an additional token.

Response: `409 Conflict` when the requested token cannot be used in the current
question, including trying to use RAG after GraphRAG or when no token remains.

### Load a clue

- `GET /api/v1/games/{gameId}/clues/question` returns the question movie clue.
- `GET /api/v1/games/{gameId}/clues/connection` returns the connecting movie
  clue revealed by GraphRAG.

Response:

```json
{
  "movie": "Open Range",
  "actors": [
    "Robert Duvall",
    "Kevin Costner",
    "Annette Bening",
    "Michael Gambon",
    "Michael Jeter"
  ]
}
```

Response: `409 Conflict` when the corresponding clue has not been unlocked.

## High scores

`GET /api/v1/high-scores`

Response: `200 OK` with up to three entries ordered by score descending:

```json
[
  { "player": "Chris", "score": 8 },
  { "player": "Jane", "score": 6 },
  { "player": "John", "score": 5 }
]
```

## Error format

Errors follow the Problem Details format. Validation errors additionally have
an `errors` object keyed by request field.

```json
{
  "type": "about:blank",
  "title": "Invalid request",
  "status": 400,
  "detail": "One or more request fields are invalid.",
  "instance": "/api/v1/games",
  "errors": {
    "player": "Player name is required."
  }
}
```

Common status codes are:

- `400 Bad Request` for malformed or invalid input.
- `404 Not Found` for a missing or expired game.
- `409 Conflict` when an action is not valid for the current game state.
- `500 Internal Server Error` for unexpected server failures.
