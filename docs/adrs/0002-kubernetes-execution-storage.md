# ADR 0002: Kubernetes Execution Storage

- Status: accepted
- Date: 2026-03-13

## Context

ConstructraOS is shifting specialist execution away from branch-scoped Docker Compose environments toward execution-scoped Kubernetes environments. That shift makes file storage an explicit architecture concern.

The repo docs and current execution model already establish these constraints:

- GitHub should become the source of truth for branch, push, PR, and merge workflows
- execution environments should be isolated per run rather than shared across all branch work
- bootstrap project state still lives behind the repo-backed markdown work-tracking boundary
- specialist environments are expected to be disposable, rebuildable, and safe to replace
- durable evidence such as QA outputs, logs, screenshots, and other execution artifacts still needs a stable home outside a single pod filesystem

If ConstructraOS treats a shared writable Kubernetes volume as the main workspace model, the platform will take on avoidable coupling around concurrent writers, cleanup, storage-class differences, and stale branch state.

## Decision

ConstructraOS will use a hybrid storage model for Kubernetes-backed specialist execution:

- GitHub remains the authoritative source of truth for source code and branch state.
- Each execution-scoped environment clones or checks out the requested repo ref into pod-local ephemeral workspace storage.
- Durable execution outputs such as logs, screenshots, reports, and bundled evidence artifacts go to S3-compatible object storage behind a dedicated ConstructraOS storage boundary.
- API and orchestration persist artifact metadata and object references, not large blobs or shared filesystem paths.
- Shared RWX persistent volumes are not the default workspace model and should be used only as an explicit optimization for non-authoritative caches when justified.

For local Kubernetes branch-team environments, ConstructraOS may use a single team PVC with one repo clone plus multiple `git worktree` checkouts on that volume, provided each active writer gets its own worktree and pods stay within the local same-node storage assumptions.

## Non-Goals

This ADR does not decide:

- the cloud provider or managed Kubernetes distribution
- the final long-term replacement for markdown task, bug, and ADR tracking
- the exact object-store implementation for every deployment, beyond requiring an S3-compatible boundary
- the final cache strategy for build tooling and package managers

## Consequences

Positive:

- execution environments stay disposable and easier to reason about
- branch and code truth stays aligned with GitHub instead of drifting into shared volumes
- durable artifacts can survive pod and namespace teardown
- storage concerns stay behind an explicit platform seam instead of leaking object-store details through workflows
- local branch-team collaboration can move forward without introducing RWX storage or one full clone per agent

Negative:

- each execution now needs repo checkout/bootstrap work instead of reusing one shared writable tree
- artifact upload/download and manifest management become first-class platform concerns
- local and cloud environments will need an object-store implementation, credentials, and lifecycle policy

## Follow-On

The next implementation steps are:

1. add a dedicated storage boundary for object put/get/list and artifact manifest generation
2. define the execution environment contract so Kubernetes launches receive repo ref, artifact prefix, and callback metadata
3. keep workspace storage ephemeral by default and introduce shared persistent caches only when a measured bottleneck justifies them
