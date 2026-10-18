import React from 'react'
import { describe, it, expect, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ConflictBanner } from '../components/common/ConflictBanner'

describe('ConflictBanner Component Suite', () => {
  it('returns null when conflictMessage is null or empty', () => {
    const { container } = render(<ConflictBanner conflictMessage={null} />)
    expect(container.firstChild).toBeNull()
  })

  it('renders conflict message and prompt when 409 error occurs', () => {
    render(
      <ConflictBanner
        conflictMessage="Work item 1 is currently claimed by another user: Jane Doe"
      />
    )

    expect(screen.getByText(/Work Item Conflict Detected \(HTTP 409\)/i)).toBeInTheDocument()
    expect(
      screen.getByText(/Work item 1 is currently claimed by another user: Jane Doe/i)
    ).toBeInTheDocument()
    expect(screen.getByText(/Another team member or concurrent process has modified/i)).toBeInTheDocument()
  })

  it('calls onRefresh when Reload Current State button is clicked', () => {
    const handleRefresh = vi.fn()
    render(
      <ConflictBanner
        conflictMessage="Optimistic locking conflict"
        onRefresh={handleRefresh}
      />
    )

    const reloadButton = screen.getByRole('button', { name: /Reload Current State/i })
    expect(reloadButton).toBeInTheDocument()
    fireEvent.click(reloadButton)
    expect(handleRefresh).toHaveBeenCalledTimes(1)
  })
})
