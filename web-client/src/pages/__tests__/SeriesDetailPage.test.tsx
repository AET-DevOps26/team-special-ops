import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { SeriesDetailPage } from '../SeriesDetailPage'

vi.mock('../../context/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('../../api/catalog', () => ({ listSeries: vi.fn(), listSeriesEpisodes: vi.fn() }))
vi.mock('../../api/progress', () => ({ getProgress: vi.fn(), updateProgress: vi.fn() }))
vi.mock('../../api/chat', () => ({ askQuestion: vi.fn() }))
vi.mock('../../api/likes', () => ({
  getLikes: vi.fn().mockResolvedValue([]),
  likeSeries: vi.fn(),
  unlikeSeries: vi.fn(),
}))

import { useAuth } from '../../context/useAuth'
import { listSeries, listSeriesEpisodes } from '../../api/catalog'
import { getProgress, updateProgress } from '../../api/progress'

const mockUseAuth = vi.mocked(useAuth)
const mockListSeries = vi.mocked(listSeries)
const mockEpisodes = vi.mocked(listSeriesEpisodes)
const mockGetProgress = vi.mocked(getProgress)
const mockUpdate = vi.mocked(updateProgress)

afterEach(() => vi.clearAllMocks())

const series = { id: 'st-1', title: 'Stranger Things', seasonsCount: 1, episodesCount: 3 }
const episodes = [
  { id: 'e1', season: 1, episodeNumber: 1, episodeIndex: 1, title: 'One', summary: 'sum-one' },
  { id: 'e2', season: 1, episodeNumber: 2, episodeIndex: 2, title: 'Two', summary: 'sum-two' },
  { id: 'e3', season: 1, episodeNumber: 3, episodeIndex: 3, title: 'Three', summary: 'sum-three' },
]

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
    <MemoryRouter initialEntries={['/series/st-1']}>
      <Routes>
        <Route path="/series/:id" element={<SeriesDetailPage />} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('SeriesDetailPage', () => {
  it('splits watched vs. locked at the progress line', async () => {
    mockListSeries.mockResolvedValue([series])
    mockEpisodes.mockResolvedValue(episodes)
    mockGetProgress.mockResolvedValue([{ seriesId: 'st-1', episodeIndex: 2, updatedAt: 'now' }])

    setup()

    expect(await screen.findByText('Stranger Things')).toBeInTheDocument()
    expect(screen.getByText('sum-one')).not.toHaveClass('spoiler')
    expect(screen.getByText('sum-three')).toHaveClass('spoiler')
    expect(screen.getByText(/hidden to avoid spoilers/i)).toBeInTheDocument()
  })

  it('moves the progress line when an episode is set as current', async () => {
    mockListSeries.mockResolvedValue([series])
    mockEpisodes.mockResolvedValue(episodes)
    mockGetProgress.mockResolvedValue([{ seriesId: 'st-1', episodeIndex: 3, updatedAt: 'now' }])
    mockUpdate.mockResolvedValue({ seriesId: 'st-1', episodeIndex: 1, updatedAt: 'now' })

    setup()
    await screen.findByText('Stranger Things')

    const buttons = screen.getAllByRole('button', { name: /set as current/i })
    await userEvent.click(buttons[0])

    expect(mockUpdate).toHaveBeenCalledWith('tok', { seriesId: 'st-1', episodeIndex: 1 })
    expect(await screen.findByText('sum-two')).toHaveClass('spoiler')
  })

  it('shows an error when episodes fail to load', async () => {
    mockListSeries.mockResolvedValue([series])
    mockEpisodes.mockRejectedValue(new Error('boom'))
    mockGetProgress.mockResolvedValue([])

    setup()

    expect(await screen.findByText(/couldn't load/i)).toBeInTheDocument()
  })

  it('defaults to the progress season and shows only that season', async () => {
    mockListSeries.mockResolvedValue([
      { id: 'st-1', title: 'Stranger Things', seasonsCount: 2, episodesCount: 4 },
    ])
    mockEpisodes.mockResolvedValue([
      { id: 'a1', season: 1, episodeNumber: 1, episodeIndex: 1, title: 'S1E1', summary: 's1e1' },
      { id: 'a2', season: 1, episodeNumber: 2, episodeIndex: 2, title: 'S1E2', summary: 's1e2' },
      { id: 'b1', season: 2, episodeNumber: 1, episodeIndex: 3, title: 'S2E1', summary: 's2e1' },
      { id: 'b2', season: 2, episodeNumber: 2, episodeIndex: 4, title: 'S2E2', summary: 's2e2' },
    ])
    mockGetProgress.mockResolvedValue([{ seriesId: 'st-1', episodeIndex: 3, updatedAt: 'now' }])

    setup()

    // The progress banner echoes the current episode's title (S2E1), so assert on a
    // non-current episode of the season (S2E2) to test the episode list itself.
    expect(await screen.findByText(/S2E2/)).toBeInTheDocument()
    expect(screen.queryByText(/S1E1/)).not.toBeInTheDocument()
  })

  it('renders the chat sidebar', async () => {
    mockListSeries.mockResolvedValue([series])
    mockEpisodes.mockResolvedValue(episodes)
    mockGetProgress.mockResolvedValue([{ seriesId: 'st-1', episodeIndex: 2, updatedAt: 'now' }])

    setup()

    expect(await screen.findByText('Ask a question')).toBeInTheDocument()
  })

  it('switches the visible season when another tab is clicked', async () => {
    mockListSeries.mockResolvedValue([
      { id: 'st-1', title: 'Stranger Things', seasonsCount: 2, episodesCount: 4 },
    ])
    mockEpisodes.mockResolvedValue([
      { id: 'a1', season: 1, episodeNumber: 1, episodeIndex: 1, title: 'S1E1', summary: 's1e1' },
      { id: 'a2', season: 1, episodeNumber: 2, episodeIndex: 2, title: 'S1E2', summary: 's1e2' },
      { id: 'b1', season: 2, episodeNumber: 1, episodeIndex: 3, title: 'S2E1', summary: 's2e1' },
      { id: 'b2', season: 2, episodeNumber: 2, episodeIndex: 4, title: 'S2E2', summary: 's2e2' },
    ])
    mockGetProgress.mockResolvedValue([{ seriesId: 'st-1', episodeIndex: 3, updatedAt: 'now' }])

    setup()
    // Defaults to season 2 (progress is S2E1). Assert on S2E2 (non-current) so the
    // progress banner's echo of the current title doesn't create a duplicate match.
    await screen.findByText(/S2E2/)

    await userEvent.click(screen.getByRole('tab', { name: /season 1/i }))
    expect(screen.getByText(/S1E2/)).toBeInTheDocument()
    expect(screen.queryByText(/S2E2/)).not.toBeInTheDocument()
  })
})
