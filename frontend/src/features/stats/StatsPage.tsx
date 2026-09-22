import { useTranslation } from 'react-i18next'
import { BarChart3, Flame, Gem, Info } from 'lucide-react'
import { useStats, useXp } from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { Tag } from '../../components/ui/Tag'
import { formatDuration } from '../../lib/dates'
import type { XpSummary } from '../../lib/types'

function XpCard({ xp }: { xp: XpSummary }) {
  const { t, i18n } = useTranslation()

  const loyaltyMultiplier = 1 + 0.05 * Math.min(Math.max(xp.loyaltyStreak - 1, 0), 10)
  const progressPct = Math.min(100, Math.round((xp.xpIntoLevel / xp.xpForNextLevel) * 100))
  const fmt = (value: number) =>
    value.toLocaleString(i18n.language?.startsWith('en') ? 'en-US' : 'es-ES', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    })

  return (
    <section className="rounded-xl border border-border bg-surface p-4 sm:p-5">
      <div className="flex items-baseline justify-between gap-3">
        <h2 className="text-[12px] font-medium uppercase tracking-wide text-ink-3">
          {t('xp.sectionTitle')}
        </h2>
        <span className="text-[13px] font-semibold text-ink">
          {t('xp.totalXp', { xp: xp.totalXp })}
        </span>
      </div>

      {xp.encountersCount === 0 ? (
        <p className="mt-2 text-[13px] text-ink-3">{t('xp.empty')}</p>
      ) : (
        <>
          <div className="mt-3 flex items-center gap-3">
            <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-peach-soft font-display text-[17px] font-bold text-peach">
              {xp.level}
            </div>
            <div className="min-w-0">
              <p className="text-[15px] font-semibold text-ink">{t(xp.titleKey)}</p>
              <p className="text-[12.5px] text-ink-3">
                {t('xp.encounters', { count: xp.encountersCount })}
              </p>
            </div>
          </div>

          <div className="mt-3">
            <div className="h-1.5 overflow-hidden rounded-full bg-surface-2">
              <div
                className="h-full rounded-full bg-peach transition-[width]"
                style={{ width: `${progressPct}%` }}
              />
            </div>
            <p className="mt-1 text-[11.5px] text-ink-3">
              {t('xp.progress', {
                current: xp.xpIntoLevel,
                needed: xp.xpForNextLevel,
                next: xp.level + 1,
              })}
            </p>
          </div>

          <div className="mt-3 flex flex-wrap gap-1.5">
            {xp.exclusiveBonusActive && (
              <Tag tone="plum">
                <Gem size={11} aria-hidden="true" />
                {t('xp.exclusiveBonus')}
              </Tag>
            )}
            {xp.loyaltyStreak > 1 && (
              <Tag tone="peach">
                <Flame size={11} aria-hidden="true" />
                {xp.loyaltyPartnerName
                  ? t('xp.streakWith', { count: xp.loyaltyStreak, name: xp.loyaltyPartnerName })
                  : t('xp.loyaltyBonus', { value: fmt(loyaltyMultiplier) })}
              </Tag>
            )}
            {xp.badges.map((badge) => (
              <Tag key={badge}>{t(badge)}</Tag>
            ))}
          </div>

          {xp.breakdown.length > 0 && (
            <div className="mt-4 border-t border-border pt-3">
              <h3 className="text-[12px] font-medium uppercase tracking-wide text-ink-3">
                {t('xp.breakdown')}
              </h3>
              <ul className="mt-1.5 space-y-1">
                {xp.breakdown.map((row) => (
                  <li
                    key={row.partnerId}
                    className="flex items-baseline justify-between gap-3 text-[13px]"
                  >
                    <span className="min-w-0 truncate text-ink-2">
                      {row.partnerName ?? t('agenda.unknownPartner')}
                      <span className="text-ink-3">
                        {' '}
                        · {t('xp.encounters', { count: row.encounters })}
                      </span>
                    </span>
                    <span className="font-semibold text-ink">{row.xp} XP</span>
                  </li>
                ))}
              </ul>
            </div>
          )}
        </>
      )}

      <details className="mt-4 border-t border-border pt-3">
        <summary className="flex cursor-pointer list-none items-center gap-1.5 text-[12.5px] font-medium text-ink-2 hover:text-ink">
          <Info size={12} aria-hidden="true" />
          {t('xp.how.title')}
        </summary>
        <ul className="mt-2 list-disc space-y-1 pl-5 text-[12px] leading-relaxed text-ink-3">
          <li>{t('xp.how.base')}</li>
          <li>{t('xp.how.age')}</li>
          <li>{t('xp.how.weight')}</li>
          <li>{t('xp.how.exclusive')}</li>
          <li>{t('xp.how.loyalty')}</li>
          <li>{t('xp.how.note')}</li>
        </ul>
      </details>

      <p className="mt-3 text-[11px] text-ink-3">{t('xp.private')}</p>
    </section>
  )
}

export function StatsPage() {
  const { t } = useTranslation()
  const { data: stats, isLoading } = useStats()
  const { data: xp } = useXp()

  if (isLoading) return <PageLoader />
  if (!stats) return null

  const rows: { label: string; value: string }[] = [
    { label: t('stats.partnersRegistered'), value: String(stats.partnersRegistered) },
    { label: t('stats.uniquePartners'), value: String(stats.uniquePartners) },
    { label: t('stats.activeRelationships'), value: String(stats.activeRelationships) },
    { label: t('stats.serious'), value: String(stats.seriousRelationships) },
    { label: t('stats.casual'), value: String(stats.casualRelationships) },
    {
      label: t('stats.longest'),
      value: stats.longestRelationship
        ? `${formatDuration(stats.longestRelationship)}${
            stats.longestRelationshipPartnerName
              ? ` (${t('stats.withName', { name: stats.longestRelationshipPartnerName })})`
              : ''
          }`
        : '—',
    },
    { label: t('stats.milestones'), value: String(stats.milestonesCount) },
    { label: t('stats.situation'), value: t(`situation.${stats.situation}`) },
    {
      label: t('stats.leaderboardPosition'),
      value: stats.leaderboardRank ? `#${stats.leaderboardRank}` : t('stats.noRank'),
    },
  ]

  return (
    <div className="mx-auto max-w-xl space-y-5">
      <header>
        <h1 className="font-display text-[21px] font-semibold text-ink">{t('stats.title')}</h1>
        <p className="text-[12.5px] text-ink-3">{t('stats.private')}</p>
      </header>

      {xp && <XpCard xp={xp} />}

      {stats.partnersRegistered === 0 ? (
        <div className="rounded-xl border border-border bg-surface">
          <EmptyState
            icon={<BarChart3 size={28} strokeWidth={1.5} />}
            title={t('stats.title')}
            body={t('stats.empty')}
          />
        </div>
      ) : (
        <dl className="divide-y divide-border rounded-xl border border-border bg-surface">
          {rows.map((row) => (
            <div key={row.label} className="flex items-baseline justify-between gap-4 px-4 py-3 sm:px-5">
              <dt className="text-[13.5px] text-ink-2">{row.label}</dt>
              <dd className="text-right text-[14px] font-semibold text-ink">{row.value}</dd>
            </div>
          ))}
        </dl>
      )}
    </div>
  )
}
