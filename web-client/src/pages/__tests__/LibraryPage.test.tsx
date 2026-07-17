import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { LibraryPage } from '../LibraryPage'

vi.mock('../../context/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('../../api/catalog', () => ({ listSeries: vi.fn() }))
vi.mock('../../api/progress', () => ({ getProgress: vi.fn() }))
vi.mock('../../api/likes', () => ({
  getLikes: vi.fn().mockResolvedValue([]),
  likeSeries: vi.fn(),
  unlikeSeries: vi.fn(),
}))

import { useAuth } from '../../context/useAuth'
import { listSeries } from '../../api/catalog'
import { getProgress } from '../../api/progress'

const mockUseAuth = vi.mocked(useAuth)
const mockListSeries = vi.mocked(listSeries)
const mockGetProgress = vi.mocked(getProgress)

afterEach(() => vi.clearAllMocks())

function setup() {
  mockUseAuth.mockReturnValue({
    user: { id: '1', email: 'demo@example.com' },
    token: 'tok',
    isLoading: false,
    login: vi.fn(),
    signup: vi.fn(),
    logout: vi.fn(),
  })
  return render(
    <MemoryRouter>
      <LibraryPage />
    </MemoryRouter>,
  )
}

describe('LibraryPage', () => {
  it('renders a card per series with joined progress', async () => {
    mockListSeries.mockResolvedValue([
      { id: 'st-1', title: 'Stranger Things', seasonsCount: 4, episodesCount: 34 },
    ])
    mockGetProgress.mockResolvedValue([
      { seriesId: 'st-1', episodeIndex: 12, updatedAt: 'now' },
    ])

    setup()

    expect(await screen.findByText('Stranger Things')).toBeInTheDocument()
    expect(screen.getByText('12 / 34')).toBeInTheDocument()
  })

  it('shows an empty message when there are no series', async () => {
    mockListSeries.mockResolvedValue([])
    mockGetProgress.mockResolvedValue([])

    setup()

    expect(await screen.findByText(/no series in the catalog yet/i)).toBeInTheDocument()
  })

  it('shows an error message when the catalog call fails', async () => {
    mockListSeries.mockRejectedValue(new Error('catalog down'))
    mockGetProgress.mockResolvedValue([])

    setup()

    expect(await screen.findByText(/couldn't load/i)).toBeInTheDocument()
  })
})
