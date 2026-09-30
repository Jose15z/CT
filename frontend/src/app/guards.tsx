import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../lib/auth'
import { PageLoader } from '../components/ui/Spinner'
import { AppShell } from './AppShell'
import { PublicLayout } from './PublicLayout'

/** Pages that exist for both audiences: the app shell when signed in, the public frame otherwise. */
export function ShellOrPublic() {
  const { user, initializing } = useAuth()
  if (initializing) return <PageLoader />
  return user ? <AppShell /> : <PublicLayout />
}

export function RequireAuth() {
  const { user, initializing } = useAuth()
  if (initializing) return <PageLoader />
  if (!user) return <Navigate to="/login" replace />
  return <Outlet />
}

export function RedirectIfAuthed() {
  const { user, initializing } = useAuth()
  if (initializing) return <PageLoader />
  if (user) return <Navigate to="/dashboard" replace />
  return <Outlet />
}
