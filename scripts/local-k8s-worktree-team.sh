#!/usr/bin/env bash

set -euo pipefail

usage() {
  cat <<'EOF'
Usage:
  local-k8s-worktree-team.sh init-team --team TEAM --repo-url URL [options]
  local-k8s-worktree-team.sh add-agent --team TEAM --agent AGENT [options]
  local-k8s-worktree-team.sh remove-agent --team TEAM --agent AGENT
  local-k8s-worktree-team.sh status --team TEAM
  local-k8s-worktree-team.sh delete-team --team TEAM

Commands:
  init-team    Create namespace, PVC, repo bootstrap job, and integration pod.
  add-agent    Create a child branch worktree and agent pod on the shared team PVC.
  remove-agent Remove the agent pod and prune its worktree through a cleanup job.
  status       Show Kubernetes resources for the team namespace.
  delete-team  Delete the team namespace and all namespaced resources.

Required tools:
  kubectl

Local cluster assumptions:
  - single-node or same-node scheduling for pods mounting the same RWO PVC
  - a default dynamic provisioner such as local-path-provisioner, OpenEBS hostpath, or similar

Examples:
  local-k8s-worktree-team.sh init-team \
    --team t-0001 \
    --repo-url https://github.com/bmj16060/constructraos.git \
    --base-branch project/constructraos/integration

  local-k8s-worktree-team.sh add-agent \
    --team t-0001 \
    --agent sre \
    --parent-branch project/constructraos/integration
EOF
}

require_kubectl() {
  command -v kubectl >/dev/null 2>&1 || {
    echo "kubectl is required" >&2
    exit 1
  }
}

require_value() {
  local name="$1"
  local value="$2"
  if [[ -z "$value" ]]; then
    echo "Missing required value: $name" >&2
    exit 1
  fi
}

sanitize_dns() {
  echo "$1" \
    | tr '[:upper:]' '[:lower:]' \
    | sed -E 's/[^a-z0-9-]+/-/g; s/^-+//; s/-+$//; s/-+/-/g'
}

team_namespace() {
  echo "team-$(sanitize_dns "$1")"
}

team_pvc() {
  echo "workspace"
}

bootstrap_job() {
  echo "bootstrap-repo"
}

integration_pod() {
  echo "integration"
}

agent_pod() {
  echo "agent-$(sanitize_dns "$1")"
}

agent_branch() {
  local team="$1"
  local agent="$2"
  echo "team/$(sanitize_dns "$team")/$(sanitize_dns "$agent")"
}

parse_common_options() {
  TEAM=""
  AGENT=""
  REPO_URL=""
  BASE_BRANCH="main"
  PARENT_BRANCH=""
  STORAGE_CLASS=""
  STORAGE_SIZE="20Gi"
  GIT_IMAGE="alpine/git:2.47.2"
  AGENT_IMAGE="alpine/git:2.47.2"
  GIT_SECRET_NAME=""
  KUBECTL_CONTEXT=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --team)
        TEAM="$2"
        shift 2
        ;;
      --agent)
        AGENT="$2"
        shift 2
        ;;
      --repo-url)
        REPO_URL="$2"
        shift 2
        ;;
      --base-branch)
        BASE_BRANCH="$2"
        shift 2
        ;;
      --parent-branch)
        PARENT_BRANCH="$2"
        shift 2
        ;;
      --storage-class)
        STORAGE_CLASS="$2"
        shift 2
        ;;
      --storage-size)
        STORAGE_SIZE="$2"
        shift 2
        ;;
      --git-image)
        GIT_IMAGE="$2"
        shift 2
        ;;
      --agent-image)
        AGENT_IMAGE="$2"
        shift 2
        ;;
      --git-secret-name)
        GIT_SECRET_NAME="$2"
        shift 2
        ;;
      --context)
        KUBECTL_CONTEXT="$2"
        shift 2
        ;;
      -h|--help)
        usage
        exit 0
        ;;
      *)
        echo "Unknown option: $1" >&2
        usage >&2
        exit 1
        ;;
    esac
  done
}

kubectl_cmd() {
  if [[ -n "$KUBECTL_CONTEXT" ]]; then
    kubectl --context "$KUBECTL_CONTEXT" "$@"
  else
    kubectl "$@"
  fi
}

apply_namespace_and_pvc() {
  local namespace="$1"
  local storage_class_yaml=""
  if [[ -n "$STORAGE_CLASS" ]]; then
    storage_class_yaml="  storageClassName: ${STORAGE_CLASS}"
  fi

  kubectl_cmd apply -f - <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: ${namespace}
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: $(team_pvc)
  namespace: ${namespace}
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: ${STORAGE_SIZE}
${storage_class_yaml}
EOF
}

apply_bootstrap_job() {
  local namespace="$1"
  local secret_volume=""
  local secret_mount=""
  local git_ssh_setup="true"

  if [[ -n "$GIT_SECRET_NAME" ]]; then
    secret_volume=$(cat <<EOF
        - name: git-credentials
          secret:
            secretName: ${GIT_SECRET_NAME}
            defaultMode: 0400
EOF
)
    secret_mount=$(cat <<EOF
            - name: git-credentials
              mountPath: /git-secret
              readOnly: true
EOF
)
    git_ssh_setup=$(cat <<'EOF'
mkdir -p /root/.ssh
cp /git-secret/* /root/.ssh/ 2>/dev/null || true
chmod 700 /root/.ssh
chmod 600 /root/.ssh/* 2>/dev/null || true
EOF
)
  fi

  kubectl_cmd apply -f - <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: $(bootstrap_job)
  namespace: ${namespace}
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      volumes:
        - name: workspace
          persistentVolumeClaim:
            claimName: $(team_pvc)
${secret_volume}
      containers:
        - name: bootstrap
          image: ${GIT_IMAGE}
          command:
            - /bin/sh
            - -lc
            - |
              set -euo pipefail
              ${git_ssh_setup}
              mkdir -p /workspace/repo /workspace/worktrees /workspace/locks
              if [ ! -d /workspace/repo/.git ]; then
                rm -rf /workspace/repo/*
                git clone --branch "${BASE_BRANCH}" --single-branch "${REPO_URL}" /workspace/repo
              fi
              cd /workspace/repo
              git fetch --all --prune
              git checkout "${BASE_BRANCH}"
              git pull --ff-only origin "${BASE_BRANCH}"
              if [ ! -d /workspace/worktrees/integration/.git ]; then
                git worktree add /workspace/worktrees/integration "${BASE_BRANCH}"
              fi
          volumeMounts:
            - name: workspace
              mountPath: /workspace
${secret_mount}
EOF

  kubectl_cmd wait --namespace "$namespace" --for=condition=complete "job/$(bootstrap_job)" --timeout=180s
}

apply_integration_pod() {
  local namespace="$1"
  kubectl_cmd apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: $(integration_pod)
  namespace: ${namespace}
  labels:
    app.kubernetes.io/name: constructraos-worktree
    constructraos.io/workspace-role: integration
spec:
  restartPolicy: Always
  volumes:
    - name: workspace
      persistentVolumeClaim:
        claimName: $(team_pvc)
  containers:
    - name: integration
      image: ${AGENT_IMAGE}
      command:
        - /bin/sh
        - -lc
        - |
          cd /workspace
          while true; do sleep 3600; done
      workingDir: /workspace
      env:
        - name: TEAM_NAME
          value: "${TEAM}"
        - name: WORKTREE_PATH
          value: /workspace
        - name: REPO_CONTROL_PATH
          value: /repo
        - name: BRANCH_NAME
          value: "${BASE_BRANCH}"
      volumeMounts:
        - name: workspace
          mountPath: /workspace
          subPath: worktrees/integration
        - name: workspace
          mountPath: /repo
          subPath: repo
EOF
}

apply_agent_job() {
  local namespace="$1"
  local branch_name
  branch_name="$(agent_branch "$TEAM" "$AGENT")"
  local parent_branch="${PARENT_BRANCH:-$BASE_BRANCH}"

  kubectl_cmd apply -f - <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: worktree-$(sanitize_dns "$AGENT")
  namespace: ${namespace}
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      volumes:
        - name: workspace
          persistentVolumeClaim:
            claimName: $(team_pvc)
      containers:
        - name: create-worktree
          image: ${GIT_IMAGE}
          command:
            - /bin/sh
            - -lc
            - |
              set -euo pipefail
              mkdir -p /workspace/locks
              lockdir="/workspace/locks/worktree.lock"
              while ! mkdir "\${lockdir}" 2>/dev/null; do
                sleep 1
              done
              trap 'rmdir "\${lockdir}"' EXIT
              cd /workspace/repo
              git fetch --all --prune
              if [ -d /workspace/worktrees/$(sanitize_dns "$AGENT")/.git ]; then
                git worktree remove --force /workspace/worktrees/$(sanitize_dns "$AGENT") || true
              fi
              git branch -D "${branch_name}" 2>/dev/null || true
              git worktree add /workspace/worktrees/$(sanitize_dns "$AGENT") -b "${branch_name}" "${parent_branch}"
          volumeMounts:
            - name: workspace
              mountPath: /workspace
EOF

  kubectl_cmd wait --namespace "$namespace" --for=condition=complete "job/worktree-$(sanitize_dns "$AGENT")" --timeout=180s
}

apply_agent_pod() {
  local namespace="$1"
  local branch_name
  branch_name="$(agent_branch "$TEAM" "$AGENT")"

  kubectl_cmd apply -f - <<EOF
apiVersion: v1
kind: Pod
metadata:
  name: $(agent_pod "$AGENT")
  namespace: ${namespace}
  labels:
    app.kubernetes.io/name: constructraos-worktree
    constructraos.io/workspace-role: agent
    constructraos.io/agent-id: $(sanitize_dns "$AGENT")
spec:
  restartPolicy: Always
  volumes:
    - name: workspace
      persistentVolumeClaim:
        claimName: $(team_pvc)
  containers:
    - name: agent
      image: ${AGENT_IMAGE}
      command:
        - /bin/sh
        - -lc
        - |
          cd /workspace
          while true; do sleep 3600; done
      workingDir: /workspace
      env:
        - name: TEAM_NAME
          value: "${TEAM}"
        - name: AGENT_ID
          value: "$(sanitize_dns "$AGENT")"
        - name: WORKTREE_PATH
          value: /workspace
        - name: REPO_CONTROL_PATH
          value: /repo
        - name: BRANCH_NAME
          value: "${branch_name}"
      volumeMounts:
        - name: workspace
          mountPath: /workspace
          subPath: worktrees/$(sanitize_dns "$AGENT")
        - name: workspace
          mountPath: /repo
          subPath: repo
EOF
}

remove_agent_resources() {
  local namespace="$1"
  kubectl_cmd delete pod --namespace "$namespace" "$(agent_pod "$AGENT")" --ignore-not-found

  kubectl_cmd apply -f - <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: cleanup-$(sanitize_dns "$AGENT")
  namespace: ${namespace}
spec:
  ttlSecondsAfterFinished: 300
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      volumes:
        - name: workspace
          persistentVolumeClaim:
            claimName: $(team_pvc)
      containers:
        - name: cleanup-worktree
          image: ${GIT_IMAGE}
          command:
            - /bin/sh
            - -lc
            - |
              set -euo pipefail
              mkdir -p /workspace/locks
              lockdir="/workspace/locks/worktree.lock"
              while ! mkdir "\${lockdir}" 2>/dev/null; do
                sleep 1
              done
              trap 'rmdir "\${lockdir}"' EXIT
              cd /workspace/repo
              git worktree remove --force /workspace/worktrees/$(sanitize_dns "$AGENT") 2>/dev/null || true
              git branch -D "$(agent_branch "$TEAM" "$AGENT")" 2>/dev/null || true
          volumeMounts:
            - name: workspace
              mountPath: /workspace
EOF
}

cmd="${1:-}"
if [[ -z "$cmd" ]]; then
  usage
  exit 1
fi
shift

parse_common_options "$@"
require_kubectl

case "$cmd" in
  init-team)
    require_value "--team" "$TEAM"
    require_value "--repo-url" "$REPO_URL"
    namespace="$(team_namespace "$TEAM")"
    apply_namespace_and_pvc "$namespace"
    apply_bootstrap_job "$namespace"
    apply_integration_pod "$namespace"
    echo "Initialized local team workspace in namespace ${namespace}"
    ;;
  add-agent)
    require_value "--team" "$TEAM"
    require_value "--agent" "$AGENT"
    namespace="$(team_namespace "$TEAM")"
    apply_agent_job "$namespace"
    apply_agent_pod "$namespace"
    echo "Added agent $(sanitize_dns "$AGENT") to namespace ${namespace}"
    ;;
  remove-agent)
    require_value "--team" "$TEAM"
    require_value "--agent" "$AGENT"
    namespace="$(team_namespace "$TEAM")"
    remove_agent_resources "$namespace"
    echo "Requested removal of agent $(sanitize_dns "$AGENT") from namespace ${namespace}"
    ;;
  status)
    require_value "--team" "$TEAM"
    namespace="$(team_namespace "$TEAM")"
    kubectl_cmd get all,pvc --namespace "$namespace"
    ;;
  delete-team)
    require_value "--team" "$TEAM"
    namespace="$(team_namespace "$TEAM")"
    kubectl_cmd delete namespace "$namespace" --ignore-not-found
    echo "Deleted namespace ${namespace}"
    ;;
  -h|--help|help)
    usage
    ;;
  *)
    echo "Unknown command: $cmd" >&2
    usage >&2
    exit 1
    ;;
esac
