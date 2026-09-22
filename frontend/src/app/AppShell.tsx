import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  CalendarDays,
  Clock,
  Heart,
  Home,
  LogOut,
  Settings,
  SmilePlus,
  BarChart3,
  Trophy,
  UserCircle,
} from 'lucide-react'
import { useAuth } from '../lib/auth'
import { useMyAvatar } from '../lib/queries'
import { Avatar } from '../components/ui/Avatar'

const iconProps = { size: 17, strokeWidth: 1.9 }

function SideLink({ to, icon, label }: { to: string; icon: React.ReactNode; label: string }) {
  return (
    <NavLink
      to={to}
      className={({ isActive }) =>
        `flex items-center gap-2.5 rounded-md px-2.5 py-2 text-[13.5px] font-medium transition-colors ${
          isActive ? 'bg-peach-soft text-peach' : 'text-ink-2 hover:bg-surface-2 hover:text-ink'
        }`
      }
    >
      {icon}
      {label}
    </NavLink>
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
      <aside className="fixed inset-y-0 left-0 z-30 hidden w-56 flex-col border-r border-border bg-surface px-3 py-4 md:flex">
        <NavLink to="/dashboard" className="mb-6 flex items-center gap-2 px-2.5">
          <span className="text-[20px]" aria-hidden="true">🍑</span>
          <span className="font-display text-[16px] font-semibold tracking-tight text-ink">
            CulitosTracker
          </span>
        </NavLink>
        <nav className="flex flex-1 flex-col gap-0.5">
          <SideLink to="/dashboard" icon={<Home {...iconProps} />} label={t('nav.dashboard')} />
          <SideLink to="/partners" icon={<Heart {...iconProps} />} label={t('nav.partners')} />
          <SideLink to="/calendar" icon={<CalendarDays {...iconProps} />} label={t('nav.calendar')} />
          <SideLink to="/check-in" icon={<SmilePlus {...iconProps} />} label={t('nav.checkin')} />
          <SideLink to="/stats" icon={<BarChart3 {...iconProps} />} label={t('nav.stats')} />
          <SideLink to="/leaderboard" icon={<Trophy {...iconProps} />} label={t('nav.leaderboard')} />
          <div className="my-3 border-t border-border" />
          <SideLink to="/history" icon={<Clock {...iconProps} />} label={t('nav.history')} />
          <SideLink to="/settings" icon={<Settings {...iconProps} />} label={t('nav.settings')} />
        </nav>
        {user && (
          <div className="flex items-center gap-2.5 border-t border-border px-2 pt-3">
            <Avatar
              emoji={user.avatarEmoji}
              name={user.displayName}
              size="sm"
              imageUrl={avatarUrl}
            />
            <div className="min-w-0 flex-1">
              <p className="truncate text-[13px] font-medium text-ink">{user.displayName}</p>
              <p className="truncate text-[11.5px] text-ink-3">@{user.username}</p>
            </div>
            <button
              onClick={handleLogout}
              title={t('nav.logout')}
              aria-label={t('nav.logout')}
              className="rounded-md p-1.5 text-ink-3 hover:bg-surface-2 hover:text-ink"
            >
              <LogOut size={15} />
            </button>
          </div>
        )}
      </aside>

      {/* Mobile header */}
      <header className="sticky top-0 z-30 flex items-center justify-between border-b border-border bg-surface px-4 py-3 md:hidden">
        <NavLink to="/dashboard" className="flex items-center gap-1.5">
          <span className="text-[17px]" aria-hidden="true">🍑</span>
          <span className="font-display text-[15px] font-semibold text-ink">CulitosTracker</span>
        </NavLink>
        <NavLink to="/check-in" className="rounded-md p-2 text-ink-2" aria-label={t('nav.checkin')}>
          <SmilePlus size={19} strokeWidth={1.9} />
        </NavLink>
      </header>

      {/* Content */}
      <main className="min-w-0 flex-1 pb-20 md:ml-56 md:pb-8">
        <div className="mx-auto w-full max-w-[1000px] px-4 py-5 sm:px-6 md:py-7">
          <Outlet />
        </div>
      </main>

      {/* Mobile bottom navigation */}
      <nav className="fixed inset-x-0 bottom-0 z-30 flex border-t border-border bg-surface pb-[env(safe-area-inset-bottom)] md:hidden">
        <BottomLink to="/dashboard" icon={<Home size={19} strokeWidth={1.9} />} label={t('nav.dashboard')} />
        <BottomLink to="/partners" icon={<Heart size={19} strokeWidth={1.9} />} label={t('nav.partners')} />
        <BottomLink to="/calendar" icon={<CalendarDays size={19} strokeWidth={1.9} />} label={t('nav.calendar')} />
        <BottomLink to="/leaderboard" icon={<Trophy size={19} strokeWidth={1.9} />} label={t('nav.leaderboard')} />
        <BottomLink to="/settings" icon={<UserCircle size={19} strokeWidth={1.9} />} label={t('nav.profile')} />
      </nav>
    </div>
  )
}
