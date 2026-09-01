# Deployment

This guide describes the first production deployment shape:

- Backend: Spring Boot on Google Cloud Run.
- Frontend: Vite static build on GitHub Pages.
- Database: existing Neo4j Aura instance.

Production configuration is split between Cloud Run environment variables and Google Secret Manager. Non-secret values can be set directly as environment variables. Secret values should be stored in Secret Manager and exposed to the container as environment variables with Cloud Run `--set-secrets`, because the Spring Boot app reads them from environment variables at runtime. Do not commit production URLs or Neo4j credentials.

## Backend Environment

Required Cloud Run environment variables:

- `CORS_ALLOWED_ORIGINS` - `https://christofferbergman.github.io` (no trailing slash)
- `NEO4J_URI` - `neo4j+s://<instanceid>.databases.neo4j.io`
- `NEO4J_USERNAME` - `neo4j`
- `NEO4J_DATABASE` - `neo4j`

Required Secret Manager-backed environment variables:

- `NEO4J_PASSWORD` - Neo4j password.

These names are still environment variables inside the running container, but their values should come from Secret Manager, not from plain `--set-env-vars`.

Cloud Run provides the `PORT` environment variable automatically.

The backend container listens on Cloud Run's `PORT` environment variable and
publishes health endpoints at:

- `/actuator/health/liveness`
- `/actuator/health/readiness`

## Cloud Run Backend: First Deployment

These steps describe the first backend deployment for the `graphrag-movie-quiz` Google Cloud project. The first deployment uses Cloud Run source deployment from the local `backend/` directory.

Run these commands from the repository root unless otherwise stated.

1. Select the Google Cloud project and default Cloud Run region:

```bash
gcloud config set project graphrag-movie-quiz
gcloud config set run/region europe-west1
```

2. Enable required Google Cloud services:

```bash
gcloud services enable run.googleapis.com cloudbuild.googleapis.com secretmanager.googleapis.com artifactregistry.googleapis.com
```

3. Grant the Cloud Run Builder role to the build service account:

```bash
PROJECT_NUMBER="$(gcloud projects describe graphrag-movie-quiz \
  --format='value(projectNumber)')"

gcloud projects add-iam-policy-binding graphrag-movie-quiz \
  --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
  --role="roles/run.builder"
```

4. Create the backend configuration values in Secret Manager.

```bash
read -s NEO4J_PASSWORD_VALUE
printf %s "$NEO4J_PASSWORD_VALUE" |
  gcloud secrets create NEO4J_PASSWORD --data-file=-
unset NEO4J_PASSWORD_VALUE
```

5. Create a dedicated Cloud Run runtime service account:

```bash
gcloud iam service-accounts create graphrag-movie-quiz-backend \
  --project graphrag-movie-quiz \
  --display-name="GraphRAG Movie Quiz Backend"
```

6. Grant that service account access to the Secret Manager values:

```bash
SERVICE_ACCOUNT="graphrag-movie-quiz-backend@graphrag-movie-quiz.iam.gserviceaccount.com"

for SECRET in NEO4J_PASSWORD; do
  gcloud secrets add-iam-policy-binding "$SECRET" \
    --project graphrag-movie-quiz \
    --member="serviceAccount:${SERVICE_ACCOUNT}" \
    --role="roles/secretmanager.secretAccessor"
done
```

7. Deploy to Cloud Run:

```bash
gcloud run deploy graphrag-movie-quiz-backend \
  --source backend \
  --region europe-west1 \
  --allow-unauthenticated \
  --service-account graphrag-movie-quiz-backend@graphrag-movie-quiz.iam.gserviceaccount.com \
  --set-env-vars CORS_ALLOWED_ORIGINS="https://christofferbergman.github.io",NEO4J_URI="neo4j+s://<instanceid>.databases.neo4j.io",NEO4J_USERNAME="neo4j",NEO4J_DATABASE="neo4j" \
  --set-secrets NEO4J_PASSWORD=NEO4J_PASSWORD:latest \
  --startup-probe httpGet.path=/actuator/health/liveness,initialDelaySeconds=10,timeoutSeconds=5,periodSeconds=10,failureThreshold=6
```

In this command, `--set-env-vars` is used for deployment-specific non-secret values, while `--set-secrets` maps Secret Manager values to the environment variable names expected by the backend.

8. Copy the Cloud Run service URL from the deploy output. Use that URL in:

- GitHub repository settings: **Settings** -> **Secrets and variables** -> **Actions** -> **Variables**

Set these GitHub Actions repository variables:

```text
VITE_API_BASE_URL=https://<cloud-run-service-origin>
```

In GitHub, enter only the value in the **Value** field. For example, the `VITE_API_BASE_URL` value should be `https://<cloud-run-service-origin>`, not `VITE_API_BASE_URL=https://<cloud-run-service-origin>`, and it should not be wrapped in quotes.

These `VITE_*` values are baked into the static frontend during the GitHub Actions build. After changing any of them, rerun the `Deploy Frontend` workflow.

## Cloud Run Backend: New Revision

Use these steps when the backend code changes and the Cloud Run service already exists.

Run these commands from the repository root unless otherwise stated.

1. Make sure the project and region are selected:

```bash
gcloud config set project graphrag-movie-quiz
gcloud config set run/region europe-west1
```

2. Deploy a new revision from the current backend source:

```bash
gcloud run deploy graphrag-movie-quiz-backend \
  --source backend \
  --region europe-west1
```

Cloud Run keeps the existing service account, environment variables, and Secret Manager mappings unless they are explicitly changed. Include `--set-env-vars` or `--set-secrets` again only when changing configuration.

## GitHub Pages Frontend

1. Configure Vite's `base` as `/graph-movie-quiz/` so generated asset URLs match the GitHub Pages repository path.

2. In the GitHub repository, enable Pages with source `GitHub Actions`.

3. Add repository variables:

- `VITE_API_BASE_URL` - backend API base URL, for example `https://<cloud-run-backend-origin>`.

When creating these variables in GitHub, the variable **Name** and **Value** are separate fields. Do not include the variable name or quotes in the value.

These variables are build-time values. Updating them in GitHub does not change an already deployed Pages build; rerun the `Deploy Frontend` workflow after every change.

4. Push to `main` or run the `Deploy Frontend` workflow manually.

The workflow will build `frontend/` and publish `frontend/dist`.

## Local Development Defaults

When the backend runs with the `local` Spring profile:

- Backend listens on `http://localhost:8080`.
- Neo4j defaults to `bolt://localhost:7687`.
- Neo4j username defaults to `neo4j`.
- Neo4j password defaults to `password`.
- Neo4j database defaults to `neo4j`.
- CORS allows `http://localhost:5173`.

When the frontend runs without `VITE_API_BASE_URL`:

- Vite serves the frontend at `http://localhost:5173`.
- The frontend sends API requests to `http://localhost:8080`.
- API paths are appended by the frontend, for example `/api/v1/games`.
