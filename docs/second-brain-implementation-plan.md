# Second Brain — Scope-Aligned Implementation Plan

## 1) Product Goal (Primary Outcome)
Build an **individual-first personal memory system** that:
- Connects user knowledge sources (Notion first, then docs/Drive/WhatsApp export/import).
- Preserves original content without loss or tampering.
- Builds a graph of relationships across all notes and documents.
- Lets users open any graph node to preview/read the source note.
- Adds a native in-app note editor to continuously capture knowledge.
- Uses an AI copilot to help users navigate, resume context (“where did I leave?”), and revise quickly.

## 2) Scope Clarification
### In scope (now)
- Single-user workspaces only.
- OAuth + ingestion from Notion as first-party integration.
- In-app notes (Notion-like lightweight editor/workspace).
- Relationship graph over ingested + in-app content.
- AI assistant for traversal, summarization, and revision prompts.

### Out of scope (now)
- Multi-user collaboration, RBAC, and team knowledge sharing.
- Enterprise governance features (SSO/SCIM/audit exports).

## 3) Core Product Principles
1. **Content fidelity first**: Never alter source text; store immutable raw snapshots.
2. **Traceability**: Every graph edge and AI suggestion should be explainable to source chunks.
3. **Incremental freshness**: Prefer delta sync over full re-ingestion.
4. **Fast recall UX**: One-click from graph node to note preview + context trail.
5. **Human-controlled AI**: AI assists traversal/revision but does not silently rewrite source data.

## 4) High-Level System Design
## 4.1 Ingestion Layer
- Connectors:
  - Notion OAuth + API pull (existing base).
  - Document uploads/import pipeline (PDF/Markdown/TXT/Docx) next.
  - WhatsApp: start with export file ingestion before API-based sync.
- Modes:
  - Initial full sync after OAuth.
  - Manual sync on user action.
  - Scheduled sync (cron): recommended every 6–12 hours for personal usage.
  - Webhook/event-driven sync where provider supports it (future optimization).

## 4.2 Storage Layer
Use relational DB (existing Spring/JPA foundation) with clear boundaries:
- `source_connection` (provider tokens/metadata).
- `source_document` (immutable snapshots + provider IDs + version/hash).
- `note_document` (native notes created in app).
- `content_chunk` (chunk text + embedding + offsets).
- `graph_node`, `graph_edge` (semantic and explicit links).
- `ai_context_cache` (session memory and quick recall context).

Data protection rules:
- Store `raw_content` + normalized text separately.
- Soft delete only; never hard-delete user content unless explicit user request.
- Version each ingestion pass with timestamps and content hashes.

## 4.3 Processing + Intelligence Layer
Pipeline per changed document:
1. Extract and normalize text.
2. Chunk with deterministic strategy.
3. Generate embeddings.
4. Compute candidate relations (semantic similarity + shared entities/tags).
5. Persist graph updates with confidence scores.
6. Prepare AI summaries/revision cards (non-destructive side artifacts).

Relationship types:
- `similar_to`, `continues`, `references`, `same_topic`, `temporal_followup`.

## 4.4 Application/API Layer
Primary API groups:
- `/auth/*` — OAuth connect/disconnect.
- `/ingestion/*` — run sync, check status, list source docs.
- `/notes/*` — CRUD for native notes.
- `/graph/*` — nodes, edges, neighborhood expansion.
- `/assistant/*` — ask, revise, “resume where I left off”.

Add async job orchestration:
- Queue-based workers for ingestion/embedding to avoid request timeouts.
- Job status endpoints for frontend progress UX.

## 4.5 Frontend Experience Layer
Main flows:
1. Onboard → Connect Notion.
2. Initial sync progress view.
3. Graph view with zoom/filter + node expansion.
4. Node click → note/document preview panel.
5. In-app note editor + quick link to graph nodes.
6. AI chat panel with actions:
   - “Summarize this cluster”
   - “What did I leave unfinished?”
   - “Generate revision plan for this week”

## 5) Ingestion Frequency Decision (Recommended)
For current scope (individual users), use a hybrid approach:
- **At login**: ensure OAuth/token validity, trigger lightweight metadata refresh.
- **Manual sync button**: immediate pull for user control.
- **Scheduled cron**: every 6 hours default; user-configurable later.
- **Backoff** on API limits and run incremental sync by `last_edited_time`/cursor.

This gives freshness without high infra cost.

## 6) Roadmap (Execution Plan)
## Phase 0 — Harden Existing Foundations (1–2 weeks)
- Finalize Notion OAuth/token lifecycle handling (refresh/reconnect/errors).
- Ensure ingestion idempotency and content hashing.
- Add ingestion run logs and basic admin/debug endpoints.

## Phase 1 — Reliable Content Base (2–3 weeks)
- Implement immutable snapshot model for ingested docs.
- Add native note CRUD API + editor persistence.
- Create consistent chunking/embedding reprocessing policy for changed docs only.

## Phase 2 — Graph MVP (2 weeks)
- Build graph construction pipeline and confidence-scored edges.
- Implement node expansion API (1-hop/2-hop neighborhood).
- Node click opens note preview with source metadata.

## Phase 3 — AI Traversal + Revision (2 weeks)
- Assistant endpoint backed by retrieval over node neighborhoods.
- “Resume session” and “revision cards” generation.
- Explainability panel: show top source chunks used in each AI response.

## Phase 4 — Connector Expansion (2–4 weeks)
- Add document import connector.
- Add WhatsApp export ingestion.
- Unified connector status/settings screen.

## 7) Data Integrity + Safety Checklist
- Immutable source snapshots with version hashes.
- Provenance on every chunk and relation (`source_doc_id`, offsets).
- Audit fields: `created_at`, `updated_at`, `ingested_at`, `ingestion_run_id`.
- Encryption at rest for tokens/secrets.
- PII-safe logging (no raw sensitive content in logs).

## 8) Metrics for Success
Product metrics:
- Time-to-first-graph (from connect to usable graph).
- Recall success rate (“found what I needed”).
- Weekly revision completion rate.

System metrics:
- Ingestion success/failure rate.
- Sync latency per source.
- Graph edge precision feedback (thumbs up/down).
- AI answer grounding rate (responses with citations to source chunks).

## 9) Suggested Immediate Backlog (Next 10 Tickets)
1. Ingestion run entity + status endpoint.
2. Idempotent Notion delta sync by last edited timestamp.
3. Source snapshot table with content hash/version.
4. Native notes API (create/edit/list/get).
5. Frontend note editor persistence wiring.
6. Graph edge scoring service with threshold config.
7. Graph node preview endpoint (content + metadata).
8. Assistant retrieval pipeline over chunk embeddings.
9. “Resume where I left off” endpoint (last touched nodes + pending notes).
10. Revision plan generator (daily/weekly cards).

## 10) Decision Log (Current)
- User type: **individual only** for now.
- First-class source: **Notion**.
- Ingestion strategy: **initial full sync + manual sync + scheduled cron**.
- Storage strategy: **DB-backed context + immutable content snapshots**.
- Product identity: **memory/revision copilot, not a plain note app**.
