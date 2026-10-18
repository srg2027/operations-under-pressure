import React from 'react'
import { describe, it, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { DashboardPage } from '../pages/DashboardPage'

// Mock useSession hook
vi.mock('../context/SessionContext', () => ({
  useSession: () => ({
    currentUser: { id: 1, displayName: 'Alice Ops', email: 'alice@example.com' },
    isLoading: false,
  }),
}))

// Mock API calls
vi.mock('../api/teams', () => ({
  getTeams: vi.fn().mockResolvedValue([{ id: 10, name: 'SRE Ops', description: 'Core team' }]),
}))

vi.mock('../api/workItems', () => ({
  getAssignedWorkItems: vi.fn().mockResolvedValue([]),
  getWorkItems: vi.fn().mockResolvedValue([]),
  claimWorkItem: vi.fn(),
}))

describe('Dashboard Unavailable State Suite', () => {
  it('renders the polished unavailable state for Awaiting My Approval without inventing approval workflows', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })

    render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <DashboardPage />
        </MemoryRouter>
      </QueryClientProvider>
    )

    // Heading for panel 3
    expect(screen.getByText('Awaiting My Approval')).toBeInTheDocument()

    // Status pill and heading both contain Unavailable
    expect(screen.getAllByText(/Unavailable/i).length).toBeGreaterThanOrEqual(2)

    // Polished explanation matching frozen Newtonite spec
    expect(screen.getByText(/Approval Workflow Unavailable/i)).toBeInTheDocument()
    expect(screen.getByText(/The backend domain model implements direct state transitions/i)).toBeInTheDocument()
    expect(screen.getByText(/Complies with frozen Newtonite specification/i)).toBeInTheDocument()
  })
})
