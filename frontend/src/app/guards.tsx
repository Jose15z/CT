import { Navigate, Outlet } from 'react-router-dom'
import { useAuth } from '../lib/auth'
import { PageLoader } from '../components/ui/Spinner'

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
