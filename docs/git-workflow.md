# ConstructraOS Git Workflow

This document captures the intended git workflow for ConstructraOS.

## Workflow Model

ConstructraOS uses a direct-commit workflow unless the change is large enough to benefit from a short-lived branch.
When Codex completes a meaningful logical unit of work, it should commit that unit by default unless the user explicitly asks not to commit yet.
That normal loop also includes keeping the active branch synchronized with its remote and pushing completed work promptly instead of letting local-only commits accumulate.

## Branching Strategy

- Small, low-risk changes may commit directly to `main`.
- Larger or experimental work should use short-lived branches with the required `codex/` prefix, for example:
  - `codex/feature-<short-description>`
  - `codex/fix-<short-description>`
  - `codex/refactor-<short-description>`
  - `codex/spike-<short-description>`
- Keep branches focused and merge once the work is stable.
- Push the branch after creating it so an upstream exists from the start.

## Sync Discipline

- Prefer working on a branch that already has an upstream on `origin`.
- Before starting a larger pass, check whether the remote branch has moved and pull with rebase when needed.
- After each completed logical unit, push the branch so the remote reflects the current resume point.
- If the worktree contains unrelated dirty changes, do not let that block pushing already-committed work on the branch.
- If a push or pull is intentionally deferred, state that explicitly in the close-out.

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

## Default Loop

1. Check `git status --short`.
2. Check branch and upstream state.
3. Pull with rebase if the remote branch has advanced and reconciliation is needed before new work.
4. Complete one logical unit of work.
5. Commit that logical unit.
6. Push the branch.
7. State what was verified, what remains unverified, and whether any unrelated worktree changes remain.

## Close-Out Expectations

Before finishing a meaningful pass:

1. Check `git status --short`.
2. Check branch/upstream status.
3. Commit any completed logical unit of work unless the user asked to defer commits.
4. Push any newly created commit unless the user asked to defer pushes.
5. If no commit or no push was made, state explicitly why not.
6. Keep docs current if the platform contract changed.
7. State what you verified and what remains unverified.
