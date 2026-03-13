import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { describe, expect, it, vi } from 'vitest'
import App from './App'

function renderApp() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <App />
    </QueryClientProvider>,
  )
}

describe('App', () => {
  it('runs the hello workflow and renders the result', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL) => {
      const path = typeof input === 'string' ? input : input instanceof URL ? input.toString() : input.url

      if (path === '/config.json') {
        return new Response(
          JSON.stringify({
            serviceShortcuts: [
              {
                name: 'Temporal UI',
                href: 'http://localhost:18233',
                description: 'Workflow history, task queues, and execution inspection.',
              },
            ],
          }),
          { status: 200 },
        )
      }

      if (path === '/api/session') {
        return new Response(
          JSON.stringify({
            sessionId: 'anon-session-1',
            actorKind: 'anonymous',
            issuedAt: '2026-03-12T00:00:00Z',
          }),
          { status: 200 },
        )
      }

      if (path === '/api/workflows/hello-world/history?limit=8') {
        return new Response(JSON.stringify([]), { status: 200 })
      }

      if (path === '/api/workflows/hello-world/run') {
        return new Response(
          JSON.stringify({
            workflowId: 'wf-1',
            promptTemplate: 'starter_hello_v1',
            greeting: 'Hello from ConstructraOS.',
            provider: 'openai-compatible',
            model: 'demo',
            usage: {},
            cache: { hit: false },
            createdAt: '2026-03-12T00:00:00Z',
          }),
          { status: 200 },
        )
      }

      throw new Error(`Unexpected fetch path: ${path}`)
    })

    vi.stubGlobal('fetch', fetchMock)

    renderApp()

    const temporalLabel = await screen.findByText('Temporal UI')
    const temporalLink = temporalLabel.closest('a')
    expect(temporalLink?.getAttribute('href')).toBe('http://localhost:18233')
    expect(temporalLink?.getAttribute('target')).toBe('_blank')
    expect(temporalLink?.getAttribute('rel')).toBe('noopener noreferrer')

    await userEvent.click(screen.getByRole('button', { name: 'Run Hello Workflow' }))

    await waitFor(() => expect(screen.getByText('Hello from ConstructraOS.')).toBeTruthy())
    expect(fetchMock).toHaveBeenCalledTimes(5)
    expect(fetchMock).toHaveBeenCalledWith('/config.json')
    expect(fetchMock).toHaveBeenCalledWith('/api/session')
    expect(fetchMock).toHaveBeenCalledWith('/api/workflows/hello-world/history?limit=8')
    expect(fetchMock).toHaveBeenCalledWith('/api/workflows/hello-world/run', expect.objectContaining({ method: 'POST' }))
  })
})
