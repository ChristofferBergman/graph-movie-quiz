# GraphRAG Movie Quiz API

Java 21 and Spring Boot backend for the GraphRAG Online Movie Quiz.

## Local development

Copy `.env.example` to a suitable local secrets file or export the variables
in your shell. Run the application with the local profile:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Run the tests:

```sh
./mvnw test
```

The local profile defaults to `bolt://localhost:7687`, username `neo4j`,
password `password`, and database `neo4j`. Override these with `NEO4J_URI`,
`NEO4J_USERNAME`, `NEO4J_PASSWORD`, and `NEO4J_DATABASE`.

Production Neo4j credentials are required environment variables. The allowed
frontend origins can be supplied as a comma-separated `CORS_ALLOWED_ORIGINS`
value. Secrets must not be committed to the repository.

The planned REST contract is documented in [`../docs/api.md`](../docs/api.md).

## Production readiness

The application accepts Cloud Run's `PORT` environment variable and exposes
Actuator liveness and readiness endpoints. Container and deployment guidance is
documented in [`../docs/deployment.md`](../docs/deployment.md).
