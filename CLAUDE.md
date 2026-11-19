# CLAUDE.md

All project context for AI agents lives in `AGENTS.md` (shared with other tools) and `docs/agents/`.

@AGENTS.md

## Claude Code specifics

- At the start of a session, also read `docs/agents/decisions.md` and the latest entry of
  `docs/agents/session-log.md`.
- Before finishing a task that changed code, append an entry to `docs/agents/session-log.md`.
- Use `./mvnw -o test` (offline) when possible; dependencies are cached in `~/.m2`.
