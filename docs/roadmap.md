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
[x] Implement the API endpoint to create a game
[x] Implement question generation and persistence
[x] Implement actor autocomplete
[x] Implement answer submission, validation and score updates
[x] Add the start-game screen to the frontend
[x] Add a simple playable question-and-answer screen
[x] Verify the complete flow from the frontend through the API to Neo4j

*** Phase 4 - Complete the core game lifecycle ***
[x] Support multiple questions in the same game
[x] Implement game-over and restart behavior
[x] Restore the current game and UI state after a browser refresh
[x] Handle expired or invalid games and relevant loading and error states
[x] Make repeated requests and double-clicks safe where needed

*** Phase 5 - RAG and GraphRAG game mechanics ***
[x] Implement persistent RAG and GraphRAG token counts and usage rules
[x] Implement clue unlocking and retrieval in the backend API
[x] Implement the question graph, clue cards and card-flipping interactions
[x] Implement the RAG and GraphRAG token interactions and animations
[x] Implement clue-card zooming

*** Phase 6 - High scores ***
[x] Implement high-score qualification and storage
[x] Make high-score updates safe when games finish concurrently
[x] Implement the high-score API endpoint
[x] Add the high-score list to the frontend

*** Phase 7 - UI completion and deployment ***
[x] Complete the visual design based on the mockups and supplied assets
[x] Improve responsive layout, keyboard accessibility and reduced-motion behavior
[x] Add final frontend and backend integration tests
[ ] Configure frontend hosting and deploy the backend to Google Cloud Run
[ ] Configure production secrets, health checks, logging and basic monitoring
[ ] Run end-to-end smoke tests against the deployed application
