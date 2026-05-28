import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import { SeriesCard } from '../SeriesCard'
import type { SeriesSummary } from '../../api/catalog'

const series: SeriesSummary = {
  id: 'st-1',
  title: 'Stranger Things',
  seasonsCount: 4,
  episodesCount: 34,
}

function renderCard(progress: number) {
  return render(
    <MemoryRouter>
      <SeriesCard series={series} progress={progress} />
    </MemoryRouter>,
  )
}

describe('SeriesCard', () => {
  it('links to the series detail route', () => {
    renderCard(12)
    expect(screen.getByRole('link')).toHaveAttribute('href', '/series/st-1')
  })

  it('shows count-based progress', () => {
    renderCard(12)
    expect(screen.getByText('Stranger Things')).toBeInTheDocument()
    expect(screen.getByText(/4 seasons · 34 episodes/i)).toBeInTheDocument()
    expect(screen.getByText('12 / 34')).toBeInTheDocument()
  })

  it('shows "Not started" at zero progress', () => {
    renderCard(0)
    expect(screen.getByText(/not started/i)).toBeInTheDocument()
  })
})
