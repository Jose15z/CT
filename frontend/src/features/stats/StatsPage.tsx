import { useTranslation } from 'react-i18next'
import { BarChart3 } from 'lucide-react'
import { useStats } from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { formatDuration } from '../../lib/dates'

export function StatsPage() {
  const { t } = useTranslation()
  const { data: stats, isLoading } = useStats()

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
