# SecondBrain deployment (frontend + backend + AIML)

## 1) Configure environment
```bash
cp .env.example .env
```
Fill at minimum:
- `VITE_NOTION_CLIENT_ID`
- `OPENAI_API_KEY`
- `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` (if Google Docs auth is needed)
- `APP_CORS_ALLOWED_ORIGINS` set to frontend origin(s), not backend URL (example: `https://your-frontend.onrender.com`)

## 2) Build and run all services
```bash
docker compose up --build -d
```

## 3) Service endpoints
- Frontend: `http://localhost`
- Backend API (through frontend proxy): `http://localhost/api/*`
- AIML chat: `http://localhost:8002/chat`

## 4) Logs and shutdown
```bash
docker compose logs -f
docker compose down
```

## Notes
- Frontend is served by Nginx and proxies `/api` + `/oauth2` to the backend container.
- Backend uses Postgres in the same compose stack.
- Embeddings service is at `aiml-embeddings:8001`, wired via `AIML_BASE_URL`.
