import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { api, setSessionExpiredHandler, tokenStore } from './api'
import type { AuthResponse, User } from './types'
import { setLanguage } from '../i18n'

interface AuthContextValue {
  user: User | null
  initializing: boolean
  login: (usernameOrEmail: string, password: string) => Promise<void>
  register: (data: {
    username: string
    email: string
    password: string
    displayName: string
    preferredLanguage: string
  }) => Promise<void>
  logout: () => Promise<void>
  refreshUser: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [initializing, setInitializing] = useState(true)

  const applySession = useCallback((auth: AuthResponse) => {
    tokenStore.set(auth.accessToken, auth.refreshToken)
    setUser(auth.user)
    if (auth.user.preferredLanguage === 'en' || auth.user.preferredLanguage === 'es') {
      setLanguage(auth.user.preferredLanguage)
    }
  }, [])

  useEffect(() => {
    setSessionExpiredHandler(() => setUser(null))
    if (!tokenStore.access && !tokenStore.refresh) {
      setInitializing(false)
      return
    }
    api<User>('/api/users/me')
      .then((me) => setUser(me))
      .catch(() => setUser(null))
      .finally(() => setInitializing(false))
  }, [])

  const login = useCallback(
    async (usernameOrEmail: string, password: string) => {
      const auth = await api<AuthResponse>('/api/auth/login', {
        method: 'POST',
        body: { usernameOrEmail, password },
        auth: false,
      })
      applySession(auth)
    },
    [applySession],
  )

  const register = useCallback(
    async (data: {
      username: string
      email: string
      password: string
      displayName: string
      preferredLanguage: string
    }) => {
      const auth = await api<AuthResponse>('/api/auth/register', {
        method: 'POST',
        body: data,
        auth: false,
      })
      applySession(auth)
    },
    [applySession],
  )

  const logout = useCallback(async () => {
    const refreshToken = tokenStore.refresh
    if (refreshToken) {
      // Best-effort server-side revocation; local logout always succeeds.
      try {
        await api('/api/auth/logout', { method: 'POST', body: { refreshToken } })
      } catch {
        // ignore
      }
    }
    tokenStore.clear()
    setUser(null)
  }, [])

  const refreshUser = useCallback(async () => {
    const me = await api<User>('/api/users/me')
    setUser(me)
  }, [])

  const value = useMemo(
    () => ({ user, initializing, login, register, logout, refreshUser }),
    [user, initializing, login, register, logout, refreshUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
