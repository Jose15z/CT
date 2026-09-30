import { Link, Outlet } from 'react-router-dom'
import { LanguageToggle } from '../features/settings/LanguageToggle'

/** Wordmark + language switch shared by the pages anyone can open logged out. */
export function PublicHeader() {
  return (
    <header className="mx-auto flex max-w-3xl items-center justify-between px-5 py-4">
      <Link to="/" className="flex items-center gap-2">
        <span className="text-[20px]" aria-hidden="true">🍑</span>
        <span className="font-display text-[16px] font-semibold text-ink">CulitosTracker</span>
      </Link>
      <LanguageToggle />
    </header>
  )
}

export function PublicLayout() {
  return (
    <div className="min-h-dvh">
      <PublicHeader />
      <main className="mx-auto max-w-3xl px-5 pb-16 pt-4">
        <Outlet />
      </main>
    </div>
  )
}
