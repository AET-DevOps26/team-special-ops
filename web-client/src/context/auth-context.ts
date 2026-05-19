import { createContext } from 'react'
import type { LoginRequest, SignupRequest, UserSummary } from '../api/auth'

export interface AuthContextValue {
  user: UserSummary | null
  token: string | null
  isLoading: boolean
  login: (body: LoginRequest) => Promise<void>
  signup: (body: SignupRequest) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)
