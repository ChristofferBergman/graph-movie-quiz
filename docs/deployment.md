# Production deployment handoff

These steps require access to the selected frontend host, Google Cloud project,
Neo4j Aura instance, DNS (if used), and production monitoring. They are not run
as part of local development.

## Backend on Cloud Run

The backend container listens on Cloud Run's `PORT` environment variable and
publishes health endpoints at:

- `/actuator/health/liveness`
- `/actuator/health/readiness`

The Dockerfile runs tests while building the application image and runs the
service as a non-root user.

Create the `NEO4J_PASSWORD` secret in Secret Manager before deployment. From
the repository root, a source deployment can then be created with placeholders
replaced by production values:

```sh
gcloud run deploy graphrag-movie-quiz-api \
  --source backend \
  --region YOUR_REGION \
  --allow-unauthenticated \
  --set-env-vars NEO4J_URI=YOUR_NEO4J_URI,NEO4J_USERNAME=YOUR_NEO4J_USERNAME,NEO4J_DATABASE=neo4j,CORS_ALLOWED_ORIGINS=YOUR_FRONTEND_ORIGIN \
  --set-secrets NEO4J_PASSWORD=NEO4J_PASSWORD:latest \
  --startup-probe httpGet.path=/actuator/health/liveness,initialDelaySeconds=10,timeoutSeconds=5,periodSeconds=10,failureThreshold=6 \
  --liveness-probe httpGet.path=/actuator/health/liveness,initialDelaySeconds=20,timeoutSeconds=5,periodSeconds=30,failureThreshold=3
```

Use the Cloud Run service URL shown after deployment as the frontend's
`VITE_API_BASE_URL`.

## Frontend hosting

The frontend is a static Vite build and does not require a server-side runtime.
Create `frontend/.env.production` from `.env.production.example`, set the Cloud
Run URL, and build it:

```sh
cd frontend
npm ci
npm run build
```

Deploy the generated `frontend/dist/` directory to the selected static host.
Configure that host to serve `index.html` for the root route and use HTTPS.
If the final frontend origin differs from the value used above, update
`CORS_ALLOWED_ORIGINS` on the Cloud Run service.

## Production checks owned by the deployer

1. Confirm the readiness and liveness URLs return HTTP 200.
2. Confirm Cloud Run request/application logs are visible and create basic
   uptime and server-error alerts in Cloud Monitoring.
3. Open the hosted frontend in a clean browser and smoke-test game creation,
   autocomplete, both token types, both clue cards, a correct answer, game over,
   high scores, refresh restoration, and Close game.
4. Check the browser console and Cloud Run logs for CORS or API errors.

Google Cloud references:

- [Deploying Cloud Run services](https://docs.cloud.google.com/run/docs/deploying)
- [Cloud Run secrets](https://docs.cloud.google.com/run/docs/configuring/services/secrets)
- [Cloud Run health checks](https://docs.cloud.google.com/run/docs/configuring/healthchecks)
