# ConstructraOS Git Workflow

This document captures the intended git workflow for ConstructraOS.

## Workflow Model

ConstructraOS uses a direct-commit workflow unless the change is large enough to benefit from a short-lived branch.
When Codex completes a meaningful logical unit of work, it should commit that unit by default unless the user explicitly asks not to commit yet.

## Branching Strategy

- Small, low-risk changes may commit directly to `main`.
- Larger or experimental work should use short-lived branches with the required `codex/` prefix, for example:
  - `codex/feature-<short-description>`
  - `codex/fix-<short-description>`
  - `codex/refactor-<short-description>`
  - `codex/spike-<short-description>`
- Keep branches focused and merge once the work is stable.

## Commit Discipline

- Make small, focused commits.
- Commit after completing a logical unit of work.
- Treat the delivered repo change as a logical unit by default, even when it is docs-only, unless the user asks to leave it uncommitted.
- Do not bundle unrelated changes.
- Separate refactors from feature work when practical.
- Avoid formatting-only noise unless isolated.
- If work is intentionally left uncommitted, state that decision and the reason explicitly in the close-out.

Commit message format:

- `<type>: <concise summary>`

Examples:

- `feat: add constructraos tool guidance`
- `fix: correct workflow history query`
- `refactor: extract session bootstrap logic`

## Close-Out Expectations

Before finishing a meaningful pass:

1. Check `git status --short`.
2. Commit any completed logical unit of work unless the user asked to defer commits.
3. If no commit was made, state explicitly why not.
4. Keep docs current if the platform contract changed.
5. State what you verified and what remains unverified.
