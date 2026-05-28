import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import type { UserSummary } from '../api/auth'

const mockUser: UserSummary = {
  id: '00000000-0000-0000-0000-000000000001',
  email: 'test@example.com',
}

vi.mock('../context/useAuth', () => ({ useAuth: vi.fn() }))
vi.mock('../api/catalog', () => ({ listSeries: vi.fn(), listSeriesEpisodes: vi.fn() }))
vi.mock('../api/progress', () => ({ getProgress: vi.fn(), updateProgress: vi.fn() }))

import { useAuth } from '../context/useAuth'
import { listSeries } from '../api/catalog'
import { getProgress } from '../api/progress'

const mockUseAuth = vi.mocked(useAuth)
const mockListSeries = vi.mocked(listSeries)
const mockGetProgress = vi.mocked(getProgress)

afterEach(() => vi.clearAllMocks())

function renderAt(path: string) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <App />
    </MemoryRouter>,
  )
}

const loggedOut = {
  user: null,
  token: null,
  isLoading: false,
  login: vi.fn(),
  signup: vi.fn(),
  logout: vi.fn(),
}

describe('App routing', () => {
  it('renders login page at /login', () => {
    mockUseAuth.mockReturnValue(loggedOut)
    renderAt('/login')
    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument()
    expect(screen.getByLabelText(/email/i)).toBeInTheDocument()
  })

  it('redirects unauthenticated users from / to login', () => {
    mockUseAuth.mockReturnValue(loggedOut)
    renderAt('/')
    expect(screen.getByRole('heading', { name: /sign in/i })).toBeInTheDocument()
  })

  it('shows the library when authenticated', async () => {
    mockUseAuth.mockReturnValue({ ...loggedOut, user: mockUser, token: 'test-token' })
    mockListSeries.mockResolvedValue([])
    mockGetProgress.mockResolvedValue([])

    renderAt('/')
    expect(await screen.findByRole('heading', { name: /your shows/i })).toBeInTheDocument()
    expect(screen.getByText('test@example.com')).toBeInTheDocument()
  })

  it('login page links to signup', () => {
    mockUseAuth.mockReturnValue(loggedOut)
    renderAt('/login')
    expect(screen.getByRole('link', { name: /create an account/i })).toHaveAttribute(
      'href',
      '/signup',
    )
  })
})
