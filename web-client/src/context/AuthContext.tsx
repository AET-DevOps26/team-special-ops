import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import * as authApi from '../api/auth'
import type { LoginRequest, SignupRequest, UserSummary } from '../api/auth'
import {
  clearAccessToken,
  getAccessToken,
  setAccessToken,
} from '../lib/tokenStorage'

interface AuthContextValue {
  user: UserSummary | null
  token: string | null
  isLoading: boolean
  login: (body: LoginRequest) => Promise<void>
  signup: (body: SignupRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null)
  const [token, setToken] = useState<string | null>(() => getAccessToken())
  const [isLoading, setIsLoading] = useState(() => getAccessToken() != null)

  useEffect(() => {
    const stored = getAccessToken()
    if (!stored) {
      setIsLoading(false)
      return
    }

    let cancelled = false

    authApi
      .getMe(stored)
      .then((me) => {
        if (!cancelled) {
          setUser(me)
          setToken(stored)
        }
      })
      .catch(() => {
        if (!cancelled) {
          clearAccessToken()
          setToken(null)
          setUser(null)
        }
      })
      .finally(() => {
        if (!cancelled) setIsLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [])

  const applyAuth = useCallback((response: authApi.AuthResponse) => {
    setAccessToken(response.accessToken)
    setToken(response.accessToken)
    setUser(response.user)
  }, [])

  const login = useCallback(
    async (body: LoginRequest) => {
      const response = await authApi.login(body)
      applyAuth(response)
    },
    [applyAuth],
  )

  const signup = useCallback(
    async (body: SignupRequest) => {
      const response = await authApi.signup(body)
      applyAuth(response)
    },
    [applyAuth],
  )

  const logout = useCallback(() => {
    clearAccessToken()
    setToken(null)
    setUser(null)
  }, [])

  const value = useMemo(
    () => ({ user, token, isLoading, login, signup, logout }),
    [user, token, isLoading, login, signup, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
