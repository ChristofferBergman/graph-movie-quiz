*** Phase 1 - Project foundations ***
[x] Create backend structure with Java 21, Maven and Spring Boot
[x] Create frontend structure with React, TypeScript and Vite
[x] Install the initial frontend dependencies and React components
[x] Establish the local development configuration and test setup
[x] Inform me where to place resources (e.g. PNG images and font files) used by the frontend

*** Phase 2 - Database connection and API design ***
[x] Connect the backend to Neo4j Aura using environment-based configuration
[x] Implement the database access needed to create, load and update a game
[x] Define the REST API endpoints and their request and response payloads
[x] Configure validation, error responses and CORS for the frontend

*** Phase 3 - Minimal end-to-end game ***
[ ] Implement the API endpoint to create a game
[ ] Implement question generation and persistence
[ ] Implement actor autocomplete
[ ] Implement answer submission, validation and score updates
[ ] Add the start-game screen to the frontend
[ ] Add a simple playable question-and-answer screen
[ ] Verify the complete flow from the frontend through the API to Neo4j

*** Phase 4 - Complete the core game lifecycle ***
[ ] Support multiple questions in the same game
[ ] Implement game-over and restart behavior
[ ] Restore the current game and UI state after a browser refresh
[ ] Handle expired or invalid games and relevant loading and error states
[ ] Make repeated requests and double-clicks safe where needed

*** Phase 5 - RAG and GraphRAG game mechanics ***
[ ] Implement persistent RAG and GraphRAG token counts and usage rules
[ ] Implement clue unlocking and retrieval in the backend API
[ ] Implement the question graph, clue cards and card-flipping interactions
[ ] Implement the RAG and GraphRAG token interactions and animations
[ ] Implement clue-card zooming

*** Phase 6 - High scores ***
[ ] Implement high-score qualification and storage
[ ] Make high-score updates safe when games finish concurrently
[ ] Implement the high-score API endpoint
[ ] Add the high-score list to the frontend

*** Phase 7 - UI completion and deployment ***
[ ] Complete the visual design based on the mockups and supplied assets
[ ] Improve responsive layout, keyboard accessibility and reduced-motion behavior
[ ] Add final frontend and backend integration tests
[ ] Configure frontend hosting and deploy the backend to Google Cloud Run
[ ] Configure production secrets, health checks, logging and basic monitoring
[ ] Run end-to-end smoke tests against the deployed application
