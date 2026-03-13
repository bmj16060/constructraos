import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { backendApiClient } from './lib/api/backendApiClient'
import { fetchUiRuntimeConfig } from './lib/runtime/uiRuntimeConfig'
import './App.css'

type HelloWorldResult = {
  workflowId: string
  promptTemplate: string
  greeting: string
  provider: string
  model: string
  usage: Record<string, unknown>
  cache: Record<string, unknown>
  createdAt: string
}

type HelloHistoryEntry = {
  id: string
  workflowId: string
  name: string
  useCase: string
  greeting: string
  provider: string
  model: string
  cacheHit: boolean
  promptTemplate: string
  createdAt: string
}

type AnonymousSession = {
  sessionId: string
  actorKind: string
  issuedAt: string
}

const DEFAULT_USE_CASE =
  'Understand a new business domain, identify the first workflow slice, and shape the first durable model.'

async function runHelloWorkflow(name: string, useCase: string) {
  return backendApiClient.fetchJson<HelloWorldResult>('/api/workflows/hello-world/run', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      name,
      useCase,
      workflowId: '',
    }),
  })
}

async function fetchHistory() {
  return backendApiClient.fetchJson<HelloHistoryEntry[]>('/api/workflows/hello-world/history?limit=8')
}

async function fetchSession() {
  return backendApiClient.fetchJson<AnonymousSession>('/api/session')
}

function formatDate(value: string) {
  return new Date(value).toLocaleString()
}

export default function App() {
  const queryClient = useQueryClient()
  const [name, setName] = useState('Builder')
  const [useCase, setUseCase] = useState(DEFAULT_USE_CASE)
  const [lastResult, setLastResult] = useState<HelloWorldResult | null>(null)

  const sessionQuery = useQuery({
    queryKey: ['anonymous-session'],
    queryFn: fetchSession,
  })

  const uiRuntimeConfigQuery = useQuery({
    queryKey: ['ui-runtime-config'],
    queryFn: fetchUiRuntimeConfig,
  })

  const historyQuery = useQuery({
    queryKey: ['hello-history'],
    queryFn: fetchHistory,
  })

  const runMutation = useMutation({
    mutationFn: () => runHelloWorkflow(name.trim(), useCase.trim()),
    onSuccess: async (result) => {
      setLastResult(result)
      await queryClient.invalidateQueries({ queryKey: ['hello-history'] })
    },
  })

  return (
    <main className="page">
      <section className="hero">
        <div className="hero__content">
          <p className="eyebrow">ConstructraOS</p>
          <h1>Build the first durable slice before the project grows teeth.</h1>
          <p className="hero__lede">
            This shell runs a real Temporal workflow, calls the shared LLM activity, persists history in Postgres,
            routes policy through OPA, caches via Valkey, and emits traces to Jaeger.
          </p>
          <div className="session-chip">
            {sessionQuery.data ? (
              <>
                <span>anon session</span>
                <strong>{sessionQuery.data.sessionId.slice(0, 8)}</strong>
              </>
            ) : (
              <span>bootstrapping session...</span>
            )}
          </div>
        </div>
        <div className="stack-grid">
          <article>
            <span>API</span>
            <strong>Micronaut</strong>
          </article>
          <article>
            <span>Orchestration</span>
            <strong>Temporal</strong>
          </article>
          <article>
            <span>Persistence</span>
            <strong>Postgres</strong>
          </article>
          <article>
            <span>Policy</span>
            <strong>OPA</strong>
          </article>
          <article>
            <span>Cache</span>
            <strong>Valkey</strong>
          </article>
          <article>
            <span>Tracing</span>
            <strong>OpenTelemetry</strong>
          </article>
        </div>
        <div className="stack-shortcuts">
          <div className="stack-shortcuts__header">
            <p className="eyebrow">Stack Shortcuts</p>
            <span>Jump to the running services and observability surfaces.</span>
          </div>
          <div className="stack-shortcuts__grid">
            {uiRuntimeConfigQuery.data?.serviceShortcuts.map((shortcut) => (
              <a
                className="stack-shortcut"
                href={shortcut.href}
                key={shortcut.name}
                rel="noopener noreferrer"
                target="_blank"
              >
                <strong>{shortcut.name}</strong>
                <span>{shortcut.description}</span>
              </a>
            ))}
          </div>
        </div>
      </section>

      <section className="panel panel--form">
        <div className="panel__header">
          <div>
            <p className="eyebrow">Hello Workflow</p>
            <h2>Trigger the LLM-backed demo</h2>
          </div>
          <button
            className="primary-button"
            disabled={runMutation.isPending}
            onClick={() => void runMutation.mutateAsync()}
            type="button"
          >
            {runMutation.isPending ? 'Running...' : 'Run Hello Workflow'}
          </button>
        </div>

        <label className="field">
          <span>Your name</span>
          <input value={name} onChange={(event) => setName(event.target.value)} />
        </label>

        <label className="field">
          <span>Business problem / domain</span>
          <textarea rows={4} value={useCase} onChange={(event) => setUseCase(event.target.value)} />
        </label>

        {runMutation.isError ? (
          <p className="status status--error">
            {runMutation.error instanceof Error ? runMutation.error.message : 'Workflow run failed.'}
          </p>
        ) : null}

        {lastResult ? (
          <article className="result-card">
            <div className="result-card__meta">
              <span>{lastResult.provider}</span>
              <span>{lastResult.model}</span>
              <span>{lastResult.promptTemplate}</span>
            </div>
            <p>{lastResult.greeting}</p>
          </article>
        ) : (
          <article className="result-card result-card--empty">
            <p>Run the workflow to prove the full platform path is alive.</p>
          </article>
        )}
      </section>

      <section className="panel">
        <div className="panel__header">
          <div>
            <p className="eyebrow">History</p>
            <h2>Recent persisted runs</h2>
          </div>
        </div>

        {historyQuery.isLoading ? <p className="status">Loading workflow history...</p> : null}
        {historyQuery.isError ? <p className="status status--error">History could not be loaded.</p> : null}
        {!historyQuery.isLoading && historyQuery.data?.length === 0 ? (
          <p className="status">No runs yet. The first workflow execution will land here.</p>
        ) : null}

        <div className="history-list">
          {historyQuery.data?.map((entry) => (
            <article className="history-item" key={entry.id}>
              <div className="history-item__header">
                <strong>{entry.name}</strong>
                <span>{formatDate(entry.createdAt)}</span>
              </div>
              <p>{entry.useCase}</p>
              <p className="history-item__greeting">{entry.greeting}</p>
              <div className="history-item__meta">
                <span>{entry.provider}</span>
                <span>{entry.model}</span>
                <span>{entry.cacheHit ? 'cache hit' : 'fresh call'}</span>
              </div>
            </article>
          ))}
        </div>
      </section>
    </main>
  )
}
