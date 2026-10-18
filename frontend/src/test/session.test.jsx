import React from 'react'
import { describe, it, expect, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import { SessionProvider, useSession } from '../context/SessionContext'
import * as userApi from '../api/users'

// Test consumer component
const TestConsumer = () => {
  const { currentUser, users, selectUser, clearSession, isDemoIdentityMode } = useSession()

  return (
    <div>
      <div data-testid="current-user">{currentUser ? currentUser.displayName : 'None'}</div>
      <div data-testid="demo-mode">{isDemoIdentityMode ? 'DEMO' : 'PROD'}</div>
      <div data-testid="users-count">{users.length}</div>
      <button
        onClick={() => selectUser({ id: 99, displayName: 'Bob Tester', email: 'bob@example.com' })}
      >
        Select Bob
      </button>
      <button onClick={clearSession}>Clear</button>
    </div>
  )
}

describe('SessionContext Suite', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.restoreAllMocks()
  })

  it('initializes in demo identity mode and loads active users', async () => {
    vi.spyOn(userApi, 'getUsers').mockResolvedValue([
      { id: 1, displayName: 'Maya', email: 'maya@example.com', active: true },
    ])

    render(
      <SessionProvider>
        <TestConsumer />
      </SessionProvider>
    )

    await waitFor(() => {
      expect(screen.getByTestId('demo-mode')).toHaveTextContent('DEMO')
      expect(screen.getByTestId('users-count')).toHaveTextContent('1')
    })
  })

  it('persists selected user ID to localStorage and updates state', async () => {
    vi.spyOn(userApi, 'getUsers').mockResolvedValue([])

    render(
      <SessionProvider>
        <TestConsumer />
      </SessionProvider>
    )

    const selectButton = screen.getByText('Select Bob')
    fireEvent.click(selectButton)

    expect(screen.getByTestId('current-user')).toHaveTextContent('Bob Tester')
    expect(localStorage.getItem('operations_demo_user_id')).toBe('99')

    const clearButton = screen.getByText('Clear')
    fireEvent.click(clearButton)

    expect(screen.getByTestId('current-user')).toHaveTextContent('None')
    expect(localStorage.getItem('operations_demo_user_id')).toBeNull()
  })
})
