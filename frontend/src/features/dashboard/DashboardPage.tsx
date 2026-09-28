import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { CalendarDays, Droplet, Heart, Plus, SmilePlus } from 'lucide-react'
import {
  useDashboard,
  useEncounters,
  useStats,
  useUpcomingDatePlans,
  useXp,
} from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { Avatar } from '../../components/ui/Avatar'
import { Tag } from '../../components/ui/Tag'
import { moodEmoji, observationEmoji } from '../../lib/emoji'
import { adviceText } from '../../lib/advice'
import {
  formatDate,
  formatDayMonth,
  formatDuration,
  formatFullDay,
  toISODate,
} from '../../lib/dates'
import { LogPeriodSheet } from '../calendar/LogPeriodSheet'
import type { DashboardPartner } from '../../lib/types'

function greetingKey(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'dashboard.greetingMorning'
  if (hour < 20) return 'dashboard.greetingAfternoon'
  return 'dashboard.greetingEvening'
}

function Kpi({ label, value, detail }: { label: string; value: string; detail?: string }) {
  return (
    <div className="card px-4 py-3.5">
      <p className="text-[11px] font-medium uppercase tracking-wider text-ink-3">{label}</p>
      <p className="mt-1.5 truncate text-[22px] font-semibold leading-none tracking-tight tabular-nums text-ink">
        {value}
      </p>
      {detail && <p className="mt-1.5 truncate text-[12px] text-ink-3">{detail}</p>}
    </div>
  )
}

function KpiRow() {
  const { t } = useTranslation()
  const { data: stats } = useStats()
  const { data: xp } = useXp()
  const { data: upcoming } = useUpcomingDatePlans()
  const now = new Date()
  const monthStart = toISODate(new Date(now.getFullYear(), now.getMonth(), 1))
  const monthEnd = toISODate(new Date(now.getFullYear(), now.getMonth() + 1, 0))
  const { data: encounters } = useEncounters(monthStart, monthEnd)
  const next = upcoming?.[0]

  return (
    <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
      <Kpi
        label={t('dashboard.kpi.partners')}
        value={stats ? String(stats.activeRelationships) : '—'}
        detail={stats ? t('dashboard.kpi.registered', { count: stats.partnersRegistered }) : undefined}
      />
      <Kpi
        label={t('dashboard.kpi.level')}
        value={xp ? String(xp.level) : '—'}
        detail={xp ? `${t(xp.titleKey)} · ${xp.totalXp} XP` : undefined}
      />
      <Kpi
        label={t('dashboard.kpi.encountersMonth')}
        value={encounters ? String(encounters.length) : '—'}
        detail={
          xp && xp.loyaltyStreak > 1 && xp.loyaltyPartnerName
            ? t('xp.streakWith', { count: xp.loyaltyStreak, name: xp.loyaltyPartnerName })
            : undefined
        }
      />
      <Kpi
        label={t('dashboard.kpi.nextDate')}
        value={next ? formatDayMonth(next.date) : '—'}
        detail={next ? `${next.title}${next.partnerName ? ` · ${next.partnerName}` : ''}` : t('dashboard.kpi.none')}
      />
    </div>
  )
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <p className="text-[11px] font-medium uppercase tracking-wider text-ink-3">{children}</p>
  )
}

function PartnerToday({ partner }: { partner: DashboardPartner }) {
  const { t } = useTranslation()
  const [logOpen, setLogOpen] = useState(false)
  const displayName = partner.nickname ?? partner.name
  const cycle = partner.cycle

  return (
    <article className="card overflow-hidden">
      {/* Header: who + what kind of relationship + how long */}
      <header className="flex items-center gap-3 border-b border-border px-5 py-4">
        <Avatar emoji={partner.avatarEmoji} name={partner.name} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h2 className="text-[16px] font-semibold tracking-tight text-ink">{displayName}</h2>
            <Tag>{t(`relationshipType.${partner.relationshipType}`)}</Tag>
          </div>
          {partner.togetherSince && partner.duration && (
            <p className="mt-0.5 text-[12.5px] text-ink-3">
              {t('partner.since', { date: formatDate(partner.togetherSince) })}
              {' · '}
              {t('duration.together', { duration: formatDuration(partner.duration) })}
            </p>
          )}
        </div>
        <Link
          to={`/partners/${partner.partnerId}`}
          className="hidden h-8 items-center rounded-lg border border-border-strong bg-surface px-2.5 text-[12.5px] font-medium text-ink shadow-card transition-colors hover:bg-surface-2 sm:inline-flex"
        >
          {t('dashboard.viewRelationship')}
        </Link>
      </header>

      <div className="grid gap-x-8 gap-y-5 px-5 py-4 md:grid-cols-[1fr_240px]">
        {/* Today: check-ins, observation, advice */}
        <div className="space-y-3">
          <SectionLabel>{t('common.today')}</SectionLabel>
          <div className="space-y-1.5 text-[13.5px]">
            {partner.partnerCheckInToday && (
              <p className="text-ink">
                <span className="mr-1.5" aria-hidden="true">
                  {moodEmoji[partner.partnerCheckInToday.mood]}
                </span>
                {t('dashboard.sheReported')}:{' '}
                <span className="font-medium">{t(`mood.${partner.partnerCheckInToday.mood}`)}</span>
              </p>
            )}
            {partner.myCheckInToday ? (
              <p className="text-ink">
                <span className="mr-1.5" aria-hidden="true">
                  {moodEmoji[partner.myCheckInToday.mood]}
                </span>
                {t('dashboard.youReported')}:{' '}
                <span className="font-medium">{t(`mood.${partner.myCheckInToday.mood}`)}</span>
              </p>
            ) : (
              <p className="text-ink-3">
                {t('dashboard.noCheckInYet')}
                {' · '}
                <Link
                  to={`/check-in?partner=${partner.partnerId}`}
                  className="font-medium text-peach hover:underline"
                >
                  {t('dashboard.checkInNow')}
                </Link>
              </p>
            )}
            {partner.latestObservation && (
              <p className="text-ink-2">
                <span className="mr-1.5" aria-hidden="true">
                  {observationEmoji[partner.latestObservation.observationType]}
                </span>
                {t('dashboard.youObserved')}:{' '}
                {t(`observationType.${partner.latestObservation.observationType}`)}
              </p>
            )}
          </div>

          {partner.adviceOfTheDay && (
            <div className="rounded-lg border border-border bg-surface-2/60 px-3.5 py-3">
              <SectionLabel>{t('dashboard.adviceToday')}</SectionLabel>
              <p className="mt-1 text-[13.5px] leading-relaxed text-ink">
                {adviceText(partner.adviceOfTheDay)}
              </p>
              <p className="mt-1.5 text-[11.5px] text-ink-3">
                {t(`adviceSource.${partner.adviceOfTheDay.source}`)}
              </p>
            </div>
          )}

          {/* Anniversaries only make sense for actual relationships. */}
          {partner.nextAnniversary &&
            !['CASUAL', 'FRIENDS_WITH_BENEFITS', 'OTHER'].includes(partner.relationshipType) && (
            <p className="flex items-center gap-1.5 text-[12.5px] text-ink-2">
              <Heart size={13} className="text-plum" aria-hidden="true" />
              {t('dashboard.nextAnniversary')}: {formatDayMonth(partner.nextAnniversary.date)}
              {' · '}
              {partner.nextAnniversary.daysUntil === 0
                ? t('common.today')
                : t('common.inDays', { count: partner.nextAnniversary.daysUntil })}
            </p>
          )}
        </div>

        {/* Cycle summary */}
        <div className="border-t border-border pt-4 text-[13px] md:border-l md:border-t-0 md:pl-6 md:pt-0">
          <SectionLabel>{t('cycle.title')}</SectionLabel>
          {cycle.trackingEnabled && !cycle.insufficientData && cycle.currentPhase ? (
            <div className="mt-2 space-y-1.5">
              <div className="flex items-center gap-2">
                <Tag tone={cycle.currentPhase === 'MENSTRUATION' ? 'rose' : cycle.currentPhase === 'OVULATION' ? 'teal' : 'neutral'}>
                  {t(`phase.${cycle.currentPhase}.name`)}
                </Tag>
                {cycle.currentCycleDay && (
                  <span className="text-ink-3">
                    {t('dashboard.cycleDay', { day: cycle.currentCycleDay })}
                  </span>
                )}
              </div>
              {cycle.nextPeriodStart && (
                <p className="text-ink-2">
                  {t('dashboard.nextPeriod')}:{' '}
                  <span className="font-medium text-ink">{formatDayMonth(cycle.nextPeriodStart)}</span>
                </p>
              )}
              {cycle.ovulationDate && (
                <p className="text-ink-2">
                  {t('dashboard.ovulation')}: {formatDayMonth(cycle.ovulationDate)}
                </p>
              )}
              <p className="pt-1 text-[11px] leading-snug text-ink-3">
                {t('cycle.disclaimer.estimate')}
              </p>
            </div>
          ) : (
            <p className="mt-2 text-ink-3">
              {cycle.trackingEnabled ? t('dashboard.noCycleData') : t('dashboard.noTracking')}
            </p>
          )}
        </div>
      </div>

      {/* Actions */}
      <footer className="flex flex-wrap items-center gap-1 border-t border-border bg-surface-2/40 px-3 py-2 text-[12.5px]">
        <button
          onClick={() => setLogOpen(true)}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-medium text-rose transition-colors hover:bg-rose-soft"
        >
          <Droplet size={13} aria-hidden="true" />
          {t('dashboard.logPeriod')}
        </button>
        <Link
          to={`/check-in?partner=${partner.partnerId}`}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-medium text-ink-2 transition-colors hover:bg-surface-2 hover:text-ink"
        >
          <SmilePlus size={13} aria-hidden="true" />
          {t('nav.checkin')}
        </Link>
        <Link
          to={`/partners/${partner.partnerId}/calendar`}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-medium text-ink-2 transition-colors hover:bg-surface-2 hover:text-ink"
        >
          <CalendarDays size={13} aria-hidden="true" />
          {t('dashboard.viewCalendar')}
        </Link>
        <Link
          to={`/partners/${partner.partnerId}`}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-medium text-ink-2 transition-colors hover:bg-surface-2 hover:text-ink sm:hidden"
        >
          <Heart size={13} aria-hidden="true" />
          {t('dashboard.viewRelationship')}
        </Link>
      </footer>

      <LogPeriodSheet
        open={logOpen}
        onClose={() => setLogOpen(false)}
        partnerId={partner.partnerId}
        partnerName={displayName}
      />
    </article>
  )
}

export function DashboardPage() {
  const { t } = useTranslation()
  const { data, isLoading } = useDashboard()

  if (isLoading) return <PageLoader />
  if (!data) return null

  return (
    <div className="space-y-6">
      <PageHeader
        title={t(greetingKey(), { name: data.user.displayName.split(' ')[0] })}
        description={formatFullDay(new Date())}
        actions={
          <Link
            to="/check-in"
            className="inline-flex h-8 items-center gap-1.5 rounded-lg bg-peach px-3 text-[13px] font-medium text-on-peach shadow-[inset_0_1px_0_rgb(255_255_255/0.14),0_1px_2px_rgb(23_20_17/0.12)] transition-colors hover:bg-peach-strong"
          >
            <SmilePlus size={14} aria-hidden="true" />
            {t('dashboard.checkInNow')}
          </Link>
        }
      />

      {data.partners.length === 0 ? (
        <div className="card">
          <EmptyState
            icon={<Heart size={22} strokeWidth={1.5} />}
            title={t('dashboard.empty.title')}
            body={t('dashboard.empty.body')}
            action={
              <Link
                to="/partners"
                className="inline-flex h-9 items-center gap-1.5 rounded-lg bg-peach px-3.5 text-[13.5px] font-medium text-on-peach hover:bg-peach-strong"
              >
                <Plus size={15} />
                {t('dashboard.empty.action')}
              </Link>
            }
          />
        </div>
      ) : (
        <div className="space-y-5">
          <KpiRow />
          <div className="space-y-4">
            {data.partners.map((partner) => (
              <PartnerToday key={partner.partnerId} partner={partner} />
            ))}
          </div>
        </div>
      )}
    </div>
  )
}
