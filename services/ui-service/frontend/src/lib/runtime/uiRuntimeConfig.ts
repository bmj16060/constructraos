export type ServiceShortcut = {
  name: string
  href: string
  description: string
}

export type UiRuntimeConfig = {
  serviceShortcuts: ServiceShortcut[]
}

const DEFAULT_SERVICE_SHORTCUTS = [
  {
    name: 'API health',
    href: 'http://localhost:18080/api/healthz',
    description: 'Micronaut API boundary and session bootstrap path.',
  },
  {
    name: 'Orchestration health',
    href: 'http://localhost:18081/healthz',
    description: 'Temporal worker runtime and workflow activity host.',
  },
  {
    name: 'Policy health',
    href: 'http://localhost:18082/healthz',
    description: 'Micronaut adapter that fronts OPA policy decisions.',
  },
  {
    name: 'OPA',
    href: 'http://localhost:18181',
    description: 'Raw Open Policy Agent API and loaded Rego bundle.',
  },
  {
    name: 'Temporal UI',
    href: 'http://localhost:18233',
    description: 'Workflow history, task queues, and execution inspection.',
  },
  {
    name: 'Jaeger',
    href: 'http://localhost:18686',
    description: 'Distributed trace search across browser, API, and worker spans.',
  },
]

function normalizeShortcuts(shortcuts: unknown): ServiceShortcut[] {
  if (!Array.isArray(shortcuts)) {
    return DEFAULT_SERVICE_SHORTCUTS
  }

  const normalized = shortcuts.flatMap((shortcut) => {
    if (!shortcut || typeof shortcut !== 'object') {
      return []
    }

    const { name, href, description } = shortcut as Record<string, unknown>
    if (typeof name !== 'string' || typeof href !== 'string' || typeof description !== 'string') {
      return []
    }

    return [{ name, href, description }]
  })

  return normalized.length > 0 ? normalized : DEFAULT_SERVICE_SHORTCUTS
}

export async function fetchUiRuntimeConfig(): Promise<UiRuntimeConfig> {
  try {
    const response = await fetch('/config.json')
    if (!response.ok) {
      return { serviceShortcuts: DEFAULT_SERVICE_SHORTCUTS }
    }

    const payload = (await response.json()) as { serviceShortcuts?: unknown }
    return {
      serviceShortcuts: normalizeShortcuts(payload.serviceShortcuts),
    }
  } catch {
    return { serviceShortcuts: DEFAULT_SERVICE_SHORTCUTS }
  }
}
