import { render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { MyShowsPage } from '../MyShowsPage'

vi.mock('../../context/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('../../api/catalog', () => ({ listSeries: vi.fn() }))
vi.mock('../../api/progress', () => ({ getProgress: vi.fn() }))
vi.mock('../../api/likes', () => ({
  getLikes: vi.fn(),
  likeSeries: vi.fn(),
  unlikeSeries: vi.fn(),
}))

import { useAuth } from '../../context/useAuth'
import { listSeries } from '../../api/catalog'
import { getProgress } from '../../api/progress'
import { getLikes } from '../../api/likes'

const mockUseAuth = vi.mocked(useAuth)
const mockListSeries = vi.mocked(listSeries)
const mockGetProgress = vi.mocked(getProgress)
const mockGetLikes = vi.mocked(getLikes)

afterEach(() => vi.clearAllMocks())

const series = (id: string, title: string) => ({ id, title, seasonsCount: 1, episodesCount: 10 })

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
      <MyShowsPage />
    </MemoryRouter>,
  )
}

describe('MyShowsPage', () => {
  it('renders only liked series, in like-order (newest first) not catalog order', async () => {
    // Catalog order is Alpha, Beta, Gamma; likes are Gamma then Alpha.
    mockListSeries.mockResolvedValue([
      series('A', 'Alpha'),
      series('B', 'Beta'),
      series('C', 'Gamma'),
    ])
    mockGetProgress.mockResolvedValue([])
    mockGetLikes.mockResolvedValue(['C', 'A'])

    setup()

    await screen.findByText('Gamma')
    const titles = screen.getAllByTestId('series-title').map((el) => el.textContent)
    expect(titles).toEqual(['Gamma', 'Alpha']) // like-order, and Beta (unliked) absent
  })

  it('shows the empty state only once likes have loaded', async () => {
    mockListSeries.mockResolvedValue([series('A', 'Alpha')])
    mockGetProgress.mockResolvedValue([])
    // Likes never resolve — the page must stay in "loading", never flash the empty message.
    mockGetLikes.mockReturnValue(new Promise(() => {}))

    setup()

    await waitFor(() => expect(mockGetLikes).toHaveBeenCalled())
    expect(screen.getByText('Loading…')).toBeInTheDocument()
    expect(screen.queryByText(/haven't liked any shows/i)).not.toBeInTheDocument()
  })

  it('shows the empty message when the user has no likes', async () => {
    mockListSeries.mockResolvedValue([series('A', 'Alpha')])
    mockGetProgress.mockResolvedValue([])
    mockGetLikes.mockResolvedValue([])

    setup()

    expect(await screen.findByText(/haven't liked any shows/i)).toBeInTheDocument()
  })

  it('shows an error message when the likes call fails', async () => {
    mockListSeries.mockResolvedValue([series('A', 'Alpha')])
    mockGetProgress.mockResolvedValue([])
    mockGetLikes.mockRejectedValue(new Error('likes down'))

    setup()

    expect(await screen.findByText(/couldn't load/i)).toBeInTheDocument()
  })
})
