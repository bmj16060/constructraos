export class BackendApiError extends Error {
  readonly status: number

  constructor(path: string, status: number) {
    super(`Backend request failed for ${path} (${status})`)
    this.name = 'BackendApiError'
    this.status = status
  }
}

async function backendFetch(path: string, init?: RequestInit) {
  return init ? fetch(path, init) : fetch(path)
}

async function fetchJson<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await backendFetch(path, init)
  if (!response.ok) {
    throw new BackendApiError(path, response.status)
  }
  return (await response.json()) as T
}

export const backendApiClient = {
  fetch: backendFetch,
  fetchJson,
}
