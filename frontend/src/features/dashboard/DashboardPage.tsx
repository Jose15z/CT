import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { CalendarDays, Droplet, Heart, Plus, SmilePlus } from 'lucide-react'
import { useDashboard } from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { Avatar } from '../../components/ui/Avatar'
import { Tag } from '../../components/ui/Tag'
import { moodEmoji, observationEmoji } from '../../lib/emoji'
import { formatDate, formatDayMonth, formatDuration, formatFullDay } from '../../lib/dates'
import { LogPeriodSheet } from '../calendar/LogPeriodSheet'
import type { DashboardPartner } from '../../lib/types'

function greetingKey(): string {
  const hour = new Date().getHours()
  if (hour < 12) return 'dashboard.greetingMorning'
  if (hour < 20) return 'dashboard.greetingAfternoon'
  return 'dashboard.greetingEvening'
}

function PartnerToday({ partner }: { partner: DashboardPartner }) {
  const { t } = useTranslation()
  const [logOpen, setLogOpen] = useState(false)
  const displayName = partner.nickname ?? partner.name
  const cycle = partner.cycle

  return (
    <article className="rounded-xl border border-border bg-surface">
      {/* Header: who + what kind of relationship + how long */}
      <header className="flex items-center gap-3 px-4 pt-4 sm:px-5">
        <Avatar emoji={partner.avatarEmoji} name={partner.name} />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-baseline gap-x-2">
            <h2 className="font-display text-[17px] font-semibold text-ink">{displayName}</h2>
            <span className="text-[12.5px] text-ink-3">
              {t(`relationshipType.${partner.relationshipType}`)}
            </span>
          </div>
          {partner.togetherSince && partner.duration && (
            <p className="text-[12.5px] text-ink-2">
              {t('partner.since', { date: formatDate(partner.togetherSince) })}
              {' · '}
              {t('duration.together', { duration: formatDuration(partner.duration) })}
            </p>
          )}
        </div>
      </header>

      <div className="grid gap-x-8 gap-y-4 px-4 py-4 sm:px-5 md:grid-cols-[1fr_230px]">
        {/* Today: check-ins, observation, advice */}
        <div className="space-y-3">
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
            <div className="border-l-2 border-peach pl-3">
              <p className="text-[13.5px] leading-relaxed text-ink">
                {t(partner.adviceOfTheDay.messageKey, partner.adviceOfTheDay.params)}
              </p>
              <p className="mt-0.5 text-[11.5px] text-ink-3">
                {t(`adviceSource.${partner.adviceOfTheDay.source}`)}
              </p>
            </div>
          )}

          {/* Anniversaries only make sense for actual relationships. */}
          {partner.nextAnniversary &&
            !['CASUAL', 'FRIENDS_WITH_BENEFITS', 'OTHER'].includes(partner.relationshipType) && (
            <p className="text-[12.5px] text-ink-2">
              <Heart size={13} className="mr-1 inline-block text-plum" aria-hidden="true" />
              {t('dashboard.nextAnniversary')}: {formatDayMonth(partner.nextAnniversary.date)}
              {' · '}
              {partner.nextAnniversary.daysUntil === 0
                ? t('common.today')
                : t('common.inDays', { count: partner.nextAnniversary.daysUntil })}
            </p>
          )}
        </div>

        {/* Cycle summary */}
        <div className="border-t border-border pt-3 text-[13px] md:border-l md:border-t-0 md:pl-6 md:pt-0">
          {cycle.trackingEnabled && !cycle.insufficientData && cycle.currentPhase ? (
            <div className="space-y-1.5">
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
            <p className="text-ink-3">
              {cycle.trackingEnabled ? t('dashboard.noCycleData') : t('dashboard.noTracking')}
            </p>
          )}
        </div>
      </div>

      {/* Actions */}
      <footer className="flex flex-wrap items-center gap-1 border-t border-border px-2 py-1.5 text-[13px]">
        <button
          onClick={() => setLogOpen(true)}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-medium text-rose hover:bg-rose-soft"
        >
          <Droplet size={14} aria-hidden="true" />
          {t('dashboard.logPeriod')}
        </button>
        <Link
          to={`/check-in?partner=${partner.partnerId}`}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-medium text-ink-2 hover:bg-surface-2 hover:text-ink"
        >
          <SmilePlus size={14} aria-hidden="true" />
          {t('nav.checkin')}
        </Link>
        <Link
          to={`/partners/${partner.partnerId}`}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-medium text-ink-2 hover:bg-surface-2 hover:text-ink"
        >
          <Heart size={14} aria-hidden="true" />
          {t('dashboard.viewRelationship')}
        </Link>
        <Link
          to={`/partners/${partner.partnerId}/calendar`}
          className="flex items-center gap-1.5 rounded-md px-2.5 py-1.5 font-medium text-ink-2 hover:bg-surface-2 hover:text-ink"
        >
          <CalendarDays size={14} aria-hidden="true" />
          {t('dashboard.viewCalendar')}
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

  const todayLabel = formatFullDay(new Date())

  return (
    <div className="space-y-5">
      <header>
        <h1 className="font-display text-[21px] font-semibold text-ink">
          {t(greetingKey(), { name: data.user.displayName.split(' ')[0] })}
        </h1>
        <p className="text-[13px] text-ink-3">{todayLabel}</p>
      </header>

      {data.partners.length === 0 ? (
        <div className="rounded-xl border border-border bg-surface">
          <EmptyState
            icon={<Heart size={28} strokeWidth={1.5} />}
            title={t('dashboard.empty.title')}
            body={t('dashboard.empty.body')}
            action={
              <Link
                to="/partners"
                className="inline-flex h-9 items-center gap-1.5 rounded-md bg-peach px-3.5 text-[13.5px] font-medium text-on-peach hover:bg-peach-strong"
              >
                <Plus size={15} />
                {t('dashboard.empty.action')}
              </Link>
            }
          />
        </div>
      ) : (
        <div className="space-y-4">
          {data.partners.map((partner) => (
            <PartnerToday key={partner.partnerId} partner={partner} />
          ))}
        </div>
      )}
    </div>
  )
}
