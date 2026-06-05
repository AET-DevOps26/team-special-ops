import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { SeasonTabs } from '../SeasonTabs'

describe('SeasonTabs', () => {
  it('renders a tab per season', () => {
    render(<SeasonTabs seasons={[1, 2, 3]} selected={2} current={1} onSelect={vi.fn()} />)
    expect(screen.getAllByRole('tab')).toHaveLength(3)
  })

  it('marks the selected season', () => {
    render(<SeasonTabs seasons={[1, 2, 3]} selected={2} current={1} onSelect={vi.fn()} />)
    expect(screen.getByRole('tab', { name: /season 2/i, selected: true })).toBeInTheDocument()
  })

  it('marks the current (progress) season with a dot', () => {
    render(<SeasonTabs seasons={[1, 2, 3]} selected={2} current={1} onSelect={vi.fn()} />)
    const dot = screen.getByLabelText(/current season/i)
    expect(screen.getByRole('tab', { name: /season 1/i })).toContainElement(dot)
  })

  it('calls onSelect with the clicked season', async () => {
    const onSelect = vi.fn()
    render(<SeasonTabs seasons={[1, 2, 3]} selected={2} current={1} onSelect={onSelect} />)
    await userEvent.click(screen.getByRole('tab', { name: /season 3/i }))
    expect(onSelect).toHaveBeenCalledWith(3)
  })
})
