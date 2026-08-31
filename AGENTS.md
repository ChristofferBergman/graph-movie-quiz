# Project instructions

Before making architectural or implementation changes, read:

- `docs/product_vision.md`
- `docs/architecture.md`
- `docs/database.md`
- `docs/ui.md`
- `docs/roadmap.md`
- `docs/GraphRAG Movie Quiz mockups.pdf`

## Repository structure

- `frontend/`: React and TypeScript application built with Vite
- `backend/`: Java 21 and Spring Boot API
- `docs/`: Requirements, architecture and roadmap

## Verification

After frontend changes, run:

    cd frontend
    npm run lint
    npm test
    npm run build

After backend changes, run:

    cd backend
    ./mvnw test
