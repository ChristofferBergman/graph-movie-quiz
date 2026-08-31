The frontend is a React and TypeScript application built with Vite and
it should connect to the backend through a REST API.

The backend is a Java 21 program with Spring-boot and Maven and will be
deployed on Google Cloud Run. It serves the REST API for the frontend,
and this should be a public API as we have no login for our game.

The Neo4j instance holds both the static graph that the questions are
generated from, and also the current game state for all active games
(since we can't have any state in RAM in the Cloud Run instance).

The Neo4j credentials are set as environemnt variables for the backend
application. On Cloud Run the password is set as a secret.
