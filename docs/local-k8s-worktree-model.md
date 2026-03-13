# Local Kubernetes Worktree Model

This document defines the first local Kubernetes storage model for concurrent task or branch teams.

## Goal

Support more than one active agent on the same task or branch team without forcing them to co-edit the same checkout.

## Local Model

The local Kubernetes shape is:

- one namespace per task or branch team
- one team `ReadWriteOnce` PVC for the shared repo object store and worktree roots
- one canonical repo clone at `/workspace/repo`
- one integration worktree at `/workspace/worktrees/integration`
- one additional worktree per active writer under `/workspace/worktrees/<agent-id>`
- one pod per agent, each mounted to its own worktree path plus the shared repo control path

This is a local-first design. It assumes a single-node or same-node scheduling environment where multiple pods can mount the same `ReadWriteOnce` volume from the same node.

## Why Worktrees

`git worktree` gives multiple isolated working directories backed by one repo object store.

That means:

- agents do not clobber one another's checkout state
- clone cost and disk usage stay lower than one full clone per agent
- branch-per-agent coordination stays explicit
- the integration branch still has a canonical workspace for merge and QA preparation

## Directory Contract

```text
/workspace/
  repo/
  worktrees/
    integration/
    <agent-id>/
  locks/
```

Rules:

- `repo/` is the control repo used to create and prune worktrees
- no two agents share the same worktree path
- worktree creation and removal must be serialized
- integration uses its own worktree and is treated as the canonical team branch workspace

## Branch Contract

- team integration branch: user-selected base branch such as `project/constructraos/integration`
- agent child branch: `team/<team-id>/<agent-id>`

The integration branch remains the merge target for accepted child work.

## Pod Contract

Each agent pod gets:

- `/workspace` mounted from `subPath: worktrees/<agent-id>`
- `/repo` mounted from `subPath: repo`
- `WORKTREE_PATH=/workspace`
- `REPO_CONTROL_PATH=/repo`
- `BRANCH_NAME=<agent branch>`

The integration pod uses the same pattern with `subPath: worktrees/integration`.

## Lifecycle

1. Create the team namespace and team PVC.
2. Clone the repo into `/workspace/repo`.
3. Create the integration worktree from the selected team branch.
4. For each active writer, create a child branch worktree.
5. Start one pod per worktree.
6. Merge accepted child branches back into the integration branch.
7. Delete the namespace when the team environment is retired.

## Local Limits

This model is intentionally scoped to local Kubernetes:

- it is not the multi-node production storage answer
- it depends on same-node PVC access semantics
- it avoids RWX storage until a real need for multi-pod writable sharing appears

If later environments need multiple nodes, cross-node scheduling, or broader sharing, ConstructraOS should revisit the workspace model rather than stretch this local contract too far.

## Bootstrap Script

The first bootstrap path for this model is:

- [scripts/local-k8s-worktree-team.sh](/Users/brandonjohnson/SourceCode/ConstructraOS/scripts/local-k8s-worktree-team.sh)

That script creates the namespace, team PVC, bootstrap job, integration pod, and per-agent worktree pods using plain `kubectl`.
