# GraphRAG Movie Quiz API

Java 21 and Spring Boot backend for the GraphRAG Online Movie Quiz.

## Local development

Run the application with the local profile:

```sh
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

Run the tests:

```sh
./mvnw test
```

Neo4j configuration will be added in Phase 2. Secrets must be supplied through
environment variables and must not be committed to the repository.
