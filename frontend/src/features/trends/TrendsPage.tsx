import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { TrendingUp } from 'lucide-react'
import { useTrends } from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { PageHeader } from '../../components/ui/PageHeader'
import { SegmentedControl } from '../../components/ui/SegmentedControl'
import i18n from '../../i18n'
import { formatDate, parseISODate } from '../../lib/dates'
import { BarChart, LineChart } from './charts'
import type { Series } from './charts'
import type { Trends } from '../../lib/types'

const ME = 'var(--chart-me)'
const PARTNER = 'var(--chart-partner)'

function Stat({ label, value }: { label: string; value: string }) {
  return (
    <div className="card px-4 py-3">
      <p className="text-[11px] font-medium uppercase tracking-wider text-ink-3">{label}</p>
      <p className="mt-1 text-[20px] font-semibold tracking-tight tabular-nums text-ink">{value}</p>
    </div>
  )
}

function ChartCard({
  title,
  legend,
  children,
}: {
  title: string
  legend?: Series[]
  children: React.ReactNode
}) {
  return (
    <section className="card p-4">
      <div className="mb-2 flex flex-wrap items-center justify-between gap-2">
        <h2 className="text-[13px] font-semibold text-ink">{title}</h2>
        {legend && legend.length > 1 && (
          <ul className="flex items-center gap-3 text-[11.5px] text-ink-2">
            {legend.map((s) => (
              <li key={s.key} className="flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full" style={{ background: s.color }} aria-hidden="true" />
                {s.label}
              </li>
            ))}
          </ul>
        )}
      </div>
      {children}
    </section>
  )
}

function shortDay(iso: string): string {
  const lang = i18n.language?.startsWith('en') ? 'en-US' : 'es-ES'
  return parseISODate(iso).toLocaleDateString(lang, { day: 'numeric', month: 'short' })
}

function shortMonth(month: string, lang: string): string {
  const [y, m] = month.split('-').map(Number)
  return new Date(y, m - 1, 1).toLocaleDateString(lang.startsWith('en') ? 'en-US' : 'es-ES', {
    month: 'short',
  })
}

function DataTable({ trends }: { trends: Trends }) {
  const { t } = useTranslation()
  const rows = trends.days.filter(
    (d) => d.myMood !== null || d.partnerMood !== null || d.mySatisfaction !== null,
  )
  if (rows.length === 0) return null
  const cell = (v: number | null) => (v === null ? '—' : String(v))
  return (
    <details className="card p-4">
      <summary className="cursor-pointer text-[13px] font-medium text-ink-2 hover:text-ink">
        {t('trends.checkIns')} · {rows.length}
      </summary>
      <div className="mt-3 overflow-x-auto">
        <table className="w-full text-[12.5px]">
          <thead>
            <tr className="text-left text-[11px] font-medium uppercase tracking-wider text-ink-3">
              <th className="py-1.5 pr-3">{t('agenda.form.date')}</th>
              <th className="py-1.5 pr-3">{t('trends.mood')}</th>
              <th className="py-1.5 pr-3">{t('trends.energy')}</th>
              <th className="py-1.5 pr-3">{t('trends.stress')}</th>
              <th className="py-1.5 pr-3">{t('trends.satisfaction')}</th>
              <th className="py-1.5">{t('trends.partner')}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border tabular-nums text-ink">
            {rows.map((d) => (
              <tr key={d.date}>
                <td className="py-1.5 pr-3 text-ink-2">{formatDate(d.date)}</td>
                <td className="py-1.5 pr-3">{cell(d.myMood)}</td>
                <td className="py-1.5 pr-3">{cell(d.myEnergy)}</td>
                <td className="py-1.5 pr-3">{cell(d.myStress)}</td>
                <td className="py-1.5 pr-3">{cell(d.mySatisfaction)}</td>
                <td className="py-1.5">{cell(d.partnerMood)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </details>
  )
}

export function TrendsPage() {
  const { t, i18n } = useTranslation()
  const [weeks, setWeeks] = useState<'4' | '8' | '12'>('8')
  const { data: trends, isLoading } = useTrends(Number(weeks))

  if (isLoading) return <PageLoader />
  if (!trends) return null

  const dates = trends.days.map((d) => d.date)
  const me = (pick: (d: Trends['days'][number]) => number | null): Series => ({
    key: 'me',
    label: t('trends.me'),
    color: ME,
    values: trends.days.map(pick),
  })
  const partnerMood: Series = {
    key: 'partner',
    label: t('trends.partner'),
    color: PARTNER,
    values: trends.days.map((d) => d.partnerMood),
  }
  const moodSeries = trends.partnerCheckIns > 0 ? [me((d) => d.myMood), partnerMood] : [me((d) => d.myMood)]
  const encountersTotal = trends.encountersByMonth.reduce((sum, m) => sum + m.count, 0)
  const empty = trends.myCheckIns === 0 && trends.partnerCheckIns === 0 && encountersTotal === 0
  const fmt = (v: number | null) => (v === null ? '—' : v.toFixed(1))

  return (
    <div className="space-y-6">
      <PageHeader
        title={t('trends.title')}
        description={t('trends.subtitle')}
        actions={
          <SegmentedControl
            ariaLabel={t('trends.title')}
            options={[
              { value: '4', label: t('trends.weeks', { count: 4 }) },
              { value: '8', label: t('trends.weeks', { count: 8 }) },
              { value: '12', label: t('trends.weeks', { count: 12 }) },
            ]}
            value={weeks}
            onChange={setWeeks}
          />
        }
      />

      {empty ? (
        <div className="card">
          <EmptyState
            icon={<TrendingUp size={22} strokeWidth={1.5} />}
            title={t('trends.title')}
            body={t('trends.empty')}
          />
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3 lg:grid-cols-4">
            <Stat label={t('trends.checkIns')} value={String(trends.myCheckIns)} />
            <Stat label={t('trends.partner')} value={String(trends.partnerCheckIns)} />
            <Stat label={`${t('trends.average')} · ${t('trends.mood')}`} value={fmt(trends.myMoodAverage)} />
            <Stat
              label={`${t('trends.average')} · ${t('trends.satisfaction')}`}
              value={fmt(trends.mySatisfactionAverage)}
            />
          </div>

          <div className="grid gap-4 lg:grid-cols-2">
            <ChartCard title={`${t('trends.mood')} · ${t('trends.scale')}`} legend={moodSeries}>
              <LineChart dates={dates} series={moodSeries} formatDate={shortDay} ariaLabel={t('trends.mood')} />
              {trends.partnerCheckIns === 0 && (
                <p className="mt-2 text-[11.5px] text-ink-3">{t('trends.partnerNote')}</p>
              )}
            </ChartCard>
            <ChartCard title={`${t('trends.satisfaction')} · ${t('trends.scale')}`}>
              <LineChart
                dates={dates}
                series={[me((d) => d.mySatisfaction)]}
                formatDate={shortDay}
                ariaLabel={t('trends.satisfaction')}
              />
            </ChartCard>
            <ChartCard title={`${t('trends.energy')} · ${t('trends.scale')}`}>
              <LineChart
                dates={dates}
                series={[me((d) => d.myEnergy)]}
                formatDate={shortDay}
                ariaLabel={t('trends.energy')}
              />
            </ChartCard>
            <ChartCard title={`${t('trends.stress')} · ${t('trends.scale')}`}>
              <LineChart
                dates={dates}
                series={[me((d) => d.myStress)]}
                formatDate={shortDay}
                ariaLabel={t('trends.stress')}
              />
            </ChartCard>
          </div>

          <ChartCard title={t('trends.encountersByMonth')}>
            <BarChart
              items={trends.encountersByMonth.map((m) => ({
                label: shortMonth(m.month, i18n.language ?? 'es'),
                value: m.count,
              }))}
              color={ME}
              ariaLabel={t('trends.encountersByMonth')}
            />
          </ChartCard>

          <DataTable trends={trends} />
        </>
      )}
    </div>
  )
}
