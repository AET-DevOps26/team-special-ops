import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { ProgressBanner } from '../ProgressBanner'
import type { Episode } from '../../api/catalog'

const episodes: Episode[] = [
  { id: 'a', season: 1, episodeNumber: 1, episodeIndex: 1, title: 'Pilot', summary: 's' },
  { id: 'b', season: 2, episodeNumber: 1, episodeIndex: 2, title: 'Return', summary: 's' },
]

describe('ProgressBanner', () => {
  it('shows the current episode label when in progress', () => {
    render(<ProgressBanner episodes={episodes} progress={2} />)
    expect(screen.getByText(/Season 2, Episode 1/i)).toBeInTheDocument()
    expect(screen.getByText(/Return/)).toBeInTheDocument()
    expect(screen.getByText(/2 \/ 2 watched/i)).toBeInTheDocument()
  })

  it('shows "Not started" when progress is 0', () => {
    render(<ProgressBanner episodes={episodes} progress={0} />)
    expect(screen.getByText(/not started/i)).toBeInTheDocument()
    expect(screen.getByText(/0 \/ 2 watched/i)).toBeInTheDocument()
  })
})
