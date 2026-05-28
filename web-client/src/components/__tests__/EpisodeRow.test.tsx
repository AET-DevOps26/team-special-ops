import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { EpisodeRow } from '../EpisodeRow'
import type { Episode } from '../../api/catalog'

const ep: Episode = {
  id: 'a',
  season: 1,
  episodeNumber: 3,
  episodeIndex: 3,
  title: 'Holly, Jolly',
  summary: 'A readable summary.',
}

describe('EpisodeRow', () => {
  it('shows the summary plainly when watched', () => {
    render(<EpisodeRow episode={ep} watched isCurrent={false} onSetCurrent={vi.fn()} />)
    const summary = screen.getByText('A readable summary.')
    expect(summary).toBeInTheDocument()
    expect(summary).not.toHaveClass('spoiler')
    expect(screen.queryByText(/hidden to avoid spoilers/i)).not.toBeInTheDocument()
  })

  it('blurs the summary and warns when locked', () => {
    render(<EpisodeRow episode={ep} watched={false} isCurrent={false} onSetCurrent={vi.fn()} />)
    expect(screen.getByText('A readable summary.')).toHaveClass('spoiler')
    expect(screen.getByText(/hidden to avoid spoilers/i)).toBeInTheDocument()
  })

  it('calls onSetCurrent with the episode index when the button is clicked', async () => {
    const onSetCurrent = vi.fn()
    render(<EpisodeRow episode={ep} watched isCurrent={false} onSetCurrent={onSetCurrent} />)
    await userEvent.click(screen.getByRole('button', { name: /set as current/i }))
    expect(onSetCurrent).toHaveBeenCalledWith(3)
  })

  it('shows "You\'re here" instead of a button for the current episode', () => {
    render(<EpisodeRow episode={ep} watched isCurrent onSetCurrent={vi.fn()} />)
    expect(screen.getByText(/you're here/i)).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /set as current/i })).not.toBeInTheDocument()
  })
})
