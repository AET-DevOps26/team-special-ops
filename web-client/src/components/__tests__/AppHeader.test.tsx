import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { AppHeader } from '../AppHeader'

vi.mock('../../context/useAuth', () => ({ useAuth: vi.fn() }))
import { useAuth } from '../../context/useAuth'
const mockUseAuth = vi.mocked(useAuth)

function baseAuth(overrides = {}) {
  return {
    user: { id: '1', email: 'demo@example.com' },
    token: 'tok',
    isLoading: false,
    login: vi.fn(),
    signup: vi.fn(),
    logout: vi.fn(),
    ...overrides,
  }
}

describe('AppHeader', () => {
  it('shows the user email and the app name links home', () => {
    mockUseAuth.mockReturnValue(baseAuth())
    render(
      <MemoryRouter>
        <AppHeader />
      </MemoryRouter>,
    )
    expect(screen.getByText('demo@example.com')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /sceneit/i })).toHaveAttribute('href', '/')
  })

  it('logs out when the button is clicked', async () => {
    const logout = vi.fn()
    mockUseAuth.mockReturnValue(baseAuth({ logout }))
    render(
      <MemoryRouter>
        <AppHeader />
      </MemoryRouter>,
    )
    await userEvent.click(screen.getByRole('button', { name: /log out/i }))
    expect(logout).toHaveBeenCalledOnce()
  })
})
