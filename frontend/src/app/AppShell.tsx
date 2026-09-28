import { useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  BarChart3,
  CalendarDays,
  Clock,
  Heart,
  Home,
  LogOut,
  Monitor,
  Moon,
  Settings,
  SmilePlus,
  Sun,
  Trophy,
  UserCircle,
} from 'lucide-react'
import { useAuth } from '../lib/auth'
import { useMyAvatar } from '../lib/queries'
import { getThemePreference, setThemePreference } from '../lib/theme'
import type { ThemePreference } from '../lib/theme'
import { Avatar } from '../components/ui/Avatar'

const iconProps = { size: 15, strokeWidth: 1.75 }

function SideLink({ to, icon, label }: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex h-8 items-center gap-2.5 rounded-md px-2.5 text-[13px] font-medium transition-colors duration-150 ${
          isActive
            ? 'border border-border bg-surface text-ink shadow-card [&>svg]:text-peach'
            : 'border border-transparent text-ink-2 hover:bg-surface-2 hover:text-ink [&>svg]:text-ink-3 hover:[&>svg]:text-ink-2'
        }`
      }
    >
      {icon}
      {label}
    </NavLink>
  )
}

function NavGroup({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div>
      <p className="mb-1.5 px-2.5 text-[11px] font-medium uppercase tracking-wider text-ink-3">
        {label}
      </p>
      <div className="flex flex-col gap-0.5">{children}</div>
    </div>
  )
}

/** Sun / moon / system, persisted like the Settings page control. */
function ThemeSwitch() {
  const { t } = useTranslation()
  const [theme, setTheme] = useState<ThemePreference>(getThemePreference())
  const options: { value: ThemePreference; icon: React.ReactNode; label: string }[] = [
    { value: 'light', icon: <Sun size={13} />, label: t('settings.themeLight') },
    { value: 'dark', icon: <Moon size={13} />, label: t('settings.themeDark') },
    { value: 'system', icon: <Monitor size={13} />, label: t('settings.themeSystem') },
  ]
  return (
    <div
      role="radiogroup"
      aria-label={t('settings.theme')}
      className="inline-flex rounded-md border border-border bg-surface-2 p-0.5"
    >
      {options.map((option) => (
        <button
          key={option.value}
          role="radio"
          aria-checked={theme === option.value}
          title={option.label}
          aria-label={option.label}
          onClick={() => {
            setTheme(option.value)
            setThemePreference(option.value)
          }}
          className={`flex h-6 w-7 items-center justify-center rounded-[5px] transition-colors ${
            theme === option.value
              ? 'bg-surface text-ink shadow-card'
              : 'text-ink-3 hover:text-ink'
          }`}
        >
          {option.icon}
        </button>
      ))}
    </div>
  )
}

function BottomLink({ to, icon, label }: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex min-w-0 flex-1 flex-col items-center gap-0.5 py-2 text-[10.5px] font-medium ${
          isActive ? 'text-peach' : 'text-ink-3'
        }`
      }
    >
      {icon}
      <span className="truncate">{label}</span>
    </NavLink>
  )
}

export function AppShell() {
  const { t } = useTranslation()
  const { user, logout } = useAuth()
  const { data: avatarUrl } = useMyAvatar(!!user?.hasAvatar)
  const navigate = useNavigate()

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <div className="min-h-dvh md:flex">
      {/* Desktop sidebar */}
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-60 flex-col border-r border-border bg-bg px-3 py-4 md:flex">
        <NavLink to="/dashboard" className="mb-6 flex items-center gap-2 px-2.5">
          <span className="text-[18px] leading-none" aria-hidden="true">🍑</span>
          <span className="font-display text-[15px] font-semibold tracking-tight text-ink">
            CulitosTracker
          </span>
        </NavLink>

        <nav className="flex flex-1 flex-col gap-6">
          <NavGroup label={t('nav.groupMain')}>
            <SideLink to="/dashboard" icon={<Home {...iconProps} />} label={t('nav.dashboard')} />
            <SideLink to="/partners" icon={<Heart {...iconProps} />} label={t('nav.partners')} />
            <SideLink to="/calendar" icon={<CalendarDays {...iconProps} />} label={t('nav.calendar')} />
            <SideLink to="/check-in" icon={<SmilePlus {...iconProps} />} label={t('nav.checkin')} />
          </NavGroup>
          <NavGroup label={t('nav.groupInsights')}>
            <SideLink to="/stats" icon={<BarChart3 {...iconProps} />} label={t('nav.stats')} />
            <SideLink to="/leaderboard" icon={<Trophy {...iconProps} />} label={t('nav.leaderboard')} />
            <SideLink to="/history" icon={<Clock {...iconProps} />} label={t('nav.history')} />
          </NavGroup>
          <NavGroup label={t('nav.groupAccount')}>
            <SideLink to="/settings" icon={<Settings {...iconProps} />} label={t('nav.settings')} />
          </NavGroup>
        </nav>

        <div className="mt-6 space-y-3 border-t border-border pt-4">
          <div className="flex items-center justify-between px-1">
            <span className="text-[11px] font-medium uppercase tracking-wider text-ink-3">
              {t('settings.theme')}
            </span>
            <ThemeSwitch />
          </div>
          {user && (
            <div className="flex items-center gap-2.5 rounded-lg border border-border bg-surface px-2.5 py-2 shadow-card">
              <Avatar
                emoji={user.avatarEmoji}
                name={user.displayName}
                size="sm"
                imageUrl={avatarUrl}
              />
              <div className="min-w-0 flex-1">
                <p className="truncate text-[12.5px] font-medium text-ink">{user.displayName}</p>
                <p className="truncate text-[11px] text-ink-3">@{user.username}</p>
              </div>
              <button
                onClick={handleLogout}
                title={t('nav.logout')}
                aria-label={t('nav.logout')}
                className="rounded-md p-1.5 text-ink-3 transition-colors hover:bg-surface-2 hover:text-ink"
              >
                <LogOut size={14} />
              </button>
            </div>
          )}
        </div>
      </aside>

      {/* Mobile header */}
      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-border bg-surface/90 px-4 py-3 backdrop-blur md:hidden">
        <NavLink to="/dashboard" className="flex items-center gap-1.5">
          <span className="text-[17px] leading-none" aria-hidden="true">🍑</span>
          <span className="font-display text-[15px] font-semibold tracking-tight text-ink">
            CulitosTracker
          </span>
        </NavLink>
        <NavLink
          to="/check-in"
          className="rounded-md p-2 text-ink-2 transition-colors hover:bg-surface-2"
          aria-label={t('nav.checkin')}
        >
          <SmilePlus size={19} strokeWidth={1.75} />
        </NavLink>
      </header>

      {/* Content */}
      <main className="min-w-0 flex-1 pb-20 md:ml-60 md:pb-10">
        <div className="mx-auto w-full max-w-[1040px] px-4 py-5 sm:px-6 md:px-8 md:py-8">
          <Outlet />
        </div>
      </main>

      {/* Mobile bottom navigation */}
      <nav className="fixed inset-x-0 bottom-0 z-30 flex border-t border-border bg-surface/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden">
        <BottomLink to="/dashboard" icon={<Home size={19} strokeWidth={1.75} />} label={t('nav.dashboard')} />
        <BottomLink to="/partners" icon={<Heart size={19} strokeWidth={1.75} />} label={t('nav.partners')} />
        <BottomLink to="/calendar" icon={<CalendarDays size={19} strokeWidth={1.75} />} label={t('nav.calendar')} />
        <BottomLink to="/leaderboard" icon={<Trophy size={19} strokeWidth={1.75} />} label={t('nav.leaderboard')} />
        <BottomLink to="/settings" icon={<UserCircle size={19} strokeWidth={1.75} />} label={t('nav.profile')} />
      </nav>
    </div>
  )
}
