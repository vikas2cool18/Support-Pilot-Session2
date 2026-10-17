# SupportPilot — Sessions 1–2

Code for *AI Engineering for Java Developers*.

- **Session 1 (v0.1):** answers support questions, reports tokens and latency, streams to the browser, switches cloud ↔ local models with no code change.
- **Session 2 (v0.2):** triages tickets into a validated Java record using versioned prompt files, falls back safely on bad output, holds multi-turn conversations with memory stored in PostgreSQL, and measures triage accuracy on labelled tickets.

| | |
|---|---|
| Java | 21+ (25 recommended) |
| Spring Boot | 4.1.1 |
| Spring AI | 2.0.1 |
| Models | OpenAI (default), Anthropic (`anthropic` profile), Ollama (`local` profile) |

## 1. Check your setup

From Session 2 you also need PostgreSQL: `docker compose up -d` (starts `pgvector/pgvector:pg17` on port 5432).


```bash
bash scripts/verify-setup.sh                                       # macOS / Linux / WSL / Git Bash
powershell -ExecutionPolicy Bypass -File scripts\verify-setup.ps1  # Windows
```

Pull the local model once (about 2 GB): `ollama pull llama3.2`

## 2. Run it

```bash
# Local model (no API key needed)
mvn spring-boot:run -Dspring-boot.run.profiles=local

# OpenAI (default profile)
export OPENAI_API_KEY=sk-...          # Windows PowerShell:  $env:OPENAI_API_KEY="sk-..."
mvn spring-boot:run

# Anthropic
export ANTHROPIC_API_KEY=...
mvn spring-boot:run -Dspring-boot.run.profiles=anthropic
```

Open <http://localhost:8080> for the streaming chat page.

Change models with environment variables, not code: `OPENAI_CHAT_MODEL`, `ANTHROPIC_CHAT_MODEL`, `OLLAMA_CHAT_MODEL`.

## 3. Endpoints

| Checkpoint | Endpoint | Try it |
|---|---|---|
| B | `GET /chat?q=` | `curl "localhost:8080/chat?q=How+do+I+reset+my+password"` |
| C | `GET /chat/meta?q=` | `curl "localhost:8080/chat/meta?q=Explain+SLA+tiers"` |
| D | `GET /chat/stream?q=` (SSE) | `curl -N "localhost:8080/chat/stream?q=Write+a+welcome+message"` |
| E | `GET /models` | `curl localhost:8080/models` |
| Stretch | `GET /chat/temperature?q=&t=&runs=` | `curl "localhost:8080/chat/temperature?q=Name+our+bot&t=1.0&runs=5"` |

`requests.http` has the same calls for the IntelliJ / VS Code HTTP client.

## 4. Lab checkpoints (Git tags)

Fell behind? Jump to any checkpoint: `git checkout s01-cpC`

| Tag | State |
|---|---|
| `s01-start` | Spring Boot skeleton + Spring AI BOM, no model yet |
| `s01-cpA` | Model starters, `application.yml` with profiles, system prompt file |
| `s01-cpB` | `ChatClient` bean + `GET /chat` |
| `s01-cpC` | `ChatReply` with tokens, model and latency (`/chat/meta`) |
| `s01-cpD` | Streaming with Server-Sent Events (`/chat/stream`) + browser page |
| `s01-cpE` | `/models` endpoint to prove the provider switch |
| `s01-end` | Error hints, temperature stretch, homework runner, offline tests |

## 4b. Session 2 endpoints and tags

| Checkpoint | Endpoint | Try it |
|---|---|---|
| A | `GET /triage/prompt` | `curl localhost:8080/triage/prompt` |
| B/C | `POST /triage` (text body) | `curl -X POST localhost:8080/triage -H "Content-Type: text/plain" -d "I was charged twice"` |
| D | `POST /conversations` → `POST /conversations/{id}/messages` | see `requests.http` |
| D | `GET /conversations/{id}/messages`, `DELETE /conversations/{id}` | stored history / forget |
| E | accuracy runner | `mvn spring-boot:run -Dspring-boot.run.profiles=local,accuracy` |

| Tag | State |
|---|---|
| `s02-start` | Session 1 code + PostgreSQL in `compose.yaml` + labelled tickets in `data/` |
| `s02-cpA` | Triage prompt file, enums, `TriagePrompts`, `GET /triage/prompt` |
| `s02-cpB` | `TicketTriage` record, `TriageService` with structured output, `POST /triage` |
| `s02-cpC` | Bean Validation, safe fallback, `TriageResult` with tokens and latency |
| `s02-cpD` | Chat memory in PostgreSQL, `/conversations` endpoints |
| `s02-cpE` | `TriageAccuracyRunner` on 20 labelled tickets |
| `s02-end` | Prompt v2 (few-shot), offline tests for triage and memory |

Compare prompt versions without code changes:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local,accuracy
mvn spring-boot:run -Dspring-boot.run.profiles=local,accuracy -Dspring-boot.run.arguments=--supportpilot.triage.prompt-version=v2
```

## 5. Tests (no API key, Ollama or database needed)

```bash
mvn test
```

`FakeChatModel` stands in for a real model so the tests are fast and free.

## 6. Homework: compare three models

Run the 10 questions in `homework/questions.txt` against each model; a CSV is written to `results/`.

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local,compare
mvn spring-boot:run -Dspring-boot.run.profiles=compare             # OpenAI
mvn spring-boot:run -Dspring-boot.run.profiles=anthropic,compare   # or a second local model:
OLLAMA_CHAT_MODEL=qwen3:4b mvn spring-boot:run -Dspring-boot.run.profiles=local,compare
```

Add your 1–5 quality score in the last column, then summarise the three models in `homework/model-comparison-template.csv`.

## 7. Troubleshooting

| Symptom | Fix |
|---|---|
| `401` / authentication error | Export the API key in the same terminal that runs the app; check for spaces or quotes. |
| `Connection refused ... 11434` | Start Ollama (`ollama serve`) or `docker compose up -d`. |
| Model not found | `ollama pull llama3.2`, or check the `*_CHAT_MODEL` variable. |
| Error mentioning `temperature` | Reasoning models only accept the default temperature — use a non-reasoning model for the stretch goal. |
| Very slow first local answer | The model is loading into memory; later calls are faster. Try a smaller model. |
| `Connection to localhost:5432 refused` at startup | PostgreSQL is not running: `docker compose up -d`. From Session 2 the app needs it to start. |
| Triage always returns `fallback: true` | Read the WARN log line from `TriageService`; try `supportpilot.triage.native-structured-output=false` for models without native JSON support. |
| Port 8080 in use | `mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8081` |

## Optional: Maven wrapper

To add `./mvnw` so participants don't need Maven installed: `mvn -N wrapper:wrapper`, then commit the generated files.
