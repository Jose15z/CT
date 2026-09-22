import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft, ChevronLeft, ChevronRight, Droplet, Heart } from 'lucide-react'
import { useMilestones, usePartner, usePredictions } from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { Tag } from '../../components/ui/Tag'
import { Button } from '../../components/ui/Button'
import { LogPeriodSheet } from './LogPeriodSheet'
import {
  formatDate,
  formatMonthYear,
  parseISODate,
  toISODate,
  todayISO,
  weekdayInitials,
} from '../../lib/dates'
import type { CycleDay, Milestone } from '../../lib/types'

interface DayEvent {
  kind: 'milestone' | 'anniversary'
  label: string
}

export function PartnerCalendarPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  const { data: partner, isLoading } = usePartner(id)
  const { data: milestones } = useMilestones(id)

  const [month, setMonth] = useState(() => {
    const now = new Date()
    return new Date(now.getFullYear(), now.getMonth(), 1)
  })
  const [selected, setSelected] = useState<string>(todayISO())
  const [logOpen, setLogOpen] = useState(false)

  const monthStart = toISODate(month)
  const monthEnd = toISODate(new Date(month.getFullYear(), month.getMonth() + 1, 0))
  const { data: predictions } = usePredictions(id, monthStart, monthEnd)

  const dayInfo = useMemo(() => {
    const map = new Map<string, CycleDay>()
    predictions?.days.forEach((day) => map.set(day.date, day))
    return map
  }, [predictions])

  const eventsByDate = useMemo(() => {
    const map = new Map<string, DayEvent[]>()
    const push = (date: string, event: DayEvent) => {
      map.set(date, [...(map.get(date) ?? []), event])
    }
    milestones?.forEach((milestone: Milestone) => push(milestone.date, {
      kind: 'milestone',
      label: milestone.title,
    }))
    // Anniversary: same month/day as the reference date, any year after it.
    const rel = partner?.relationship
    const base = rel?.marriageDate ?? rel?.togetherSince
    if (base && rel?.status === 'ACTIVE') {
      const baseDate = parseISODate(base)
      if (
        baseDate.getMonth() === month.getMonth() &&
        month.getFullYear() > baseDate.getFullYear()
      ) {
        const years = month.getFullYear() - baseDate.getFullYear()
        push(toISODate(new Date(month.getFullYear(), month.getMonth(), baseDate.getDate())), {
          kind: 'anniversary',
          label: t('dashboard.anniversaryYears', { years }),
        })
      }
    }
    return map
  }, [milestones, partner, month, t])

  if (isLoading) return <PageLoader />
  if (!partner) return null

  const today = todayISO()
  const firstWeekday = (month.getDay() + 6) % 7 // Monday-first
  const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate()
  const cells: (string | null)[] = [
    ...Array.from({ length: firstWeekday }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) =>
      toISODate(new Date(month.getFullYear(), month.getMonth(), i + 1)),
    ),
  ]

  const selectedInfo = dayInfo.get(selected)
  const selectedEvents = eventsByDate.get(selected) ?? []

  const dayClasses = (iso: string): string => {
    const info = dayInfo.get(iso)
    const classes: string[] = []
    if (info?.actualPeriod) {
      classes.push('bg-rose-soft text-rose font-semibold')
    } else if (info?.predictedPeriod) {
      classes.push('bg-rose-soft/50 text-rose')
    } else if (info?.fertile) {
      classes.push('bg-teal-soft text-teal')
    }
    if (info?.ovulation) {
      classes.push('ring-1 ring-inset ring-teal font-semibold')
    }
    return classes.join(' ')
  }

  return (
    <div className="space-y-5">
      <Link
        to={`/partners/${partner.id}`}
        className="inline-flex items-center gap-1.5 text-[13px] font-medium text-ink-3 hover:text-ink"
      >
        <ArrowLeft size={14} aria-hidden="true" />
        {partner.name}
      </Link>

      <header className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="font-display text-[21px] font-semibold text-ink">
          {t('calendar.of', { name: partner.nickname ?? partner.name })}
        </h1>
        <Button size="sm" icon={<Droplet size={13} />} onClick={() => setLogOpen(true)}>
          {t('period.log')}
        </Button>
      </header>

      <div className="grid gap-5 lg:grid-cols-[1fr_280px]">
        <div className="rounded-xl border border-border bg-surface p-4">
          {/* Month navigation */}
          <div className="mb-3 flex items-center justify-between">
            <button
              onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() - 1, 1))}
              aria-label="previous month"
              className="rounded-md p-1.5 text-ink-2 hover:bg-surface-2"
            >
              <ChevronLeft size={17} />
            </button>
            <div className="flex items-center gap-2">
              <span className="text-[14.5px] font-semibold text-ink">
                {formatMonthYear(month)}
              </span>
              <button
                onClick={() => {
                  const now = new Date()
                  setMonth(new Date(now.getFullYear(), now.getMonth(), 1))
                  setSelected(todayISO())
                }}
                className="rounded-md px-2 py-1 text-[12px] font-medium text-peach hover:bg-peach-soft"
              >
                {t('common.today')}
              </button>
            </div>
            <button
              onClick={() => setMonth(new Date(month.getFullYear(), month.getMonth() + 1, 1))}
              aria-label="next month"
              className="rounded-md p-1.5 text-ink-2 hover:bg-surface-2"
            >
              <ChevronRight size={17} />
            </button>
          </div>

          {/* Weekday header */}
          <div className="grid grid-cols-7 text-center text-[11px] font-medium uppercase text-ink-3">
            {weekdayInitials().map((initial, i) => (
              <div key={i} className="py-1">
                {initial}
              </div>
            ))}
          </div>

          {/* Day grid */}
          <div className="grid grid-cols-7 gap-y-0.5">
            {cells.map((iso, index) =>
              iso === null ? (
                <div key={`blank-${index}`} />
              ) : (
                <button
                  key={iso}
                  onClick={() => setSelected(iso)}
                  aria-pressed={selected === iso}
                  className={`relative mx-auto flex h-10 w-10 flex-col items-center justify-center rounded-md text-[13px] transition-colors hover:bg-surface-2 sm:h-11 sm:w-11 ${dayClasses(iso)} ${
                    selected === iso ? 'outline outline-2 outline-peach' : ''
                  } ${iso === today ? 'font-bold' : ''}`}
                >
                  <span className={iso === today ? 'text-peach' : ''}>
                    {parseISODate(iso).getDate()}
                  </span>
                  {(eventsByDate.get(iso)?.length ?? 0) > 0 && (
                    <span
                      aria-hidden="true"
                      className="absolute bottom-1 h-1 w-1 rounded-full bg-plum"
                    />
                  )}
                </button>
              ),
            )}
          </div>

          {/* Legend */}
          <div className="mt-4 flex flex-wrap gap-x-4 gap-y-1.5 border-t border-border pt-3 text-[11.5px] text-ink-2">
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded-sm bg-rose-soft ring-1 ring-inset ring-rose/40" />
              {t('calendar.legend.period')}
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded-sm bg-rose-soft/50" />
              {t('calendar.legend.predictedPeriod')}
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded-sm bg-teal-soft" />
              {t('calendar.legend.fertile')}
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-2.5 w-2.5 rounded-sm ring-1 ring-inset ring-teal" />
              {t('calendar.legend.ovulation')}
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-plum" />
              {t('calendar.legend.event')}
            </span>
          </div>
        </div>

        {/* Selected day detail */}
        <aside className="rounded-xl border border-border bg-surface p-4">
          <h2 className="text-[14px] font-semibold text-ink">{formatDate(selected)}</h2>

          {selectedInfo?.phase ? (
            <div className="mt-2 space-y-1.5 text-[13px]">
              <div className="flex items-center gap-2">
                <Tag
                  tone={
                    selectedInfo.phase === 'MENSTRUATION'
                      ? 'rose'
                      : selectedInfo.phase === 'OVULATION'
                        ? 'teal'
                        : 'neutral'
                  }
                >
                  {t(`phase.${selectedInfo.phase}.name`)}
                </Tag>
                {selectedInfo.cycleDay && (
                  <span className="text-ink-3">
                    {t('calendar.dayDetail.cycleDay', { day: selectedInfo.cycleDay })}
                  </span>
                )}
              </div>
              <p className="text-ink-2">{t(`phase.${selectedInfo.phase}.desc`)}</p>
              {selectedInfo.fertile && (
                <p className="text-teal">{t('calendar.legend.fertile')}</p>
              )}
            </div>
          ) : (
            <p className="mt-2 text-[13px] text-ink-3">{t('dashboard.noCycleData')}</p>
          )}

          <div className="mt-4 border-t border-border pt-3">
            <h3 className="text-[12px] font-medium uppercase tracking-wide text-ink-3">
              {t('calendar.dayDetail.events')}
            </h3>
            {selectedEvents.length > 0 ? (
              <ul className="mt-1.5 space-y-1">
                {selectedEvents.map((event, i) => (
                  <li key={i} className="flex items-center gap-2 text-[13px] text-ink">
                    <Heart size={12} className="text-plum" aria-hidden="true" />
                    {event.label}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-1.5 text-[13px] text-ink-3">{t('calendar.dayDetail.noEvents')}</p>
            )}
          </div>

          {selected <= today && (
            <button
              onClick={() => setLogOpen(true)}
              className="mt-4 flex items-center gap-1.5 text-[13px] font-medium text-rose hover:underline"
            >
              <Droplet size={13} aria-hidden="true" />
              {t('calendar.dayDetail.logPeriodHere')}
            </button>
          )}

          {predictions && (
            <p className="mt-4 border-t border-border pt-3 text-[11px] leading-snug text-ink-3">
              {t('cycle.disclaimer.estimate')} {t('cycle.disclaimer.notContraception')}
            </p>
          )}
        </aside>
      </div>

      <LogPeriodSheet
        open={logOpen}
        onClose={() => setLogOpen(false)}
        partnerId={partner.id}
        partnerName={partner.nickname ?? partner.name}
        initialDate={selected !== today ? selected : undefined}
      />
    </div>
  )
}
