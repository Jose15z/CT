import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { LanguageToggle } from '../settings/LanguageToggle'

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="flex min-h-dvh flex-col">
      <header className="flex items-center justify-between px-5 py-4">
        <Link to="/" className="flex items-center gap-2">
          <span className="text-[20px]" aria-hidden="true">🍑</span>
          <span className="font-display text-[16px] font-semibold text-ink">CulitosTracker</span>
        </Link>
        <LanguageToggle />
      </header>
      <main className="mx-auto w-full max-w-sm flex-1 px-5 pb-16 pt-6 sm:pt-12">
        <h1 className="font-display text-[22px] font-semibold text-ink">{title}</h1>
        <div className="mt-5">{children}</div>
      </main>
    </div>
  )
}
