import React from 'react'
import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import { StatusBadge } from '../components/common/StatusBadge'
import { PriorityBadge } from '../components/common/PriorityBadge'

describe('Badges Component Suite', () => {
  it('renders OPEN status badge correctly', () => {
    render(<StatusBadge status="OPEN" />)
    expect(screen.getByText('OPEN')).toBeInTheDocument()
  })

  it('renders IN_PROGRESS status badge correctly', () => {
    render(<StatusBadge status="IN_PROGRESS" />)
    expect(screen.getByText('IN PROGRESS')).toBeInTheDocument()
  })

  it('renders DONE status badge correctly', () => {
    render(<StatusBadge status="DONE" />)
    expect(screen.getByText('DONE')).toBeInTheDocument()
  })

  it('renders URGENT priority badge with icon and label', () => {
    render(<PriorityBadge priority="URGENT" />)
    expect(screen.getByText(/URGENT/i)).toBeInTheDocument()
  })

  it('renders HIGH priority badge correctly', () => {
    render(<PriorityBadge priority="HIGH" />)
    expect(screen.getByText(/HIGH/i)).toBeInTheDocument()
  })
})
