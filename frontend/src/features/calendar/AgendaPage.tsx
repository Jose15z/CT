import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  CalendarDays,
  CalendarPlus,
  ChevronLeft,
  ChevronRight,
  Clock3,
  Flame,
  MapPin,
  Trash2,
} from 'lucide-react'
import {
  useDatePlans,
  useDeleteDatePlan,
  useDeleteEncounter,
  useEncounters,
  usePartners,
  useUpcomingDatePlans,
} from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { Avatar } from '../../components/ui/Avatar'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { useToast } from '../../components/ui/Toast'
import { SchedulePlanSheet, LogEncounterSheet } from './AgendaSheets'
import {
  formatDate,
  formatMonthYear,
  parseISODate,
  toISODate,
  todayISO,
  weekdayInitials,
} from '../../lib/dates'

/** "20:30:00" → "20:30" */
function shortTime(time: string | null): string | null {
  return time ? time.slice(0, 5) : null
}

export function AgendaPage() {
  const { t } = useTranslation()
  const toast = useToast()
  const { data: partners, isLoading } = usePartners()

  const [month, setMonth] = useState(() => {
    const now = new Date()
    return new Date(now.getFullYear(), now.getMonth(), 1)
  })
  const [selected, setSelected] = useState<string>(todayISO())
  const [planOpen, setPlanOpen] = useState(false)
  const [logOpen, setLogOpen] = useState(false)
  const [deletePlanId, setDeletePlanId] = useState<string | null>(null)
  const [deleteEncounterId, setDeleteEncounterId] = useState<string | null>(null)

  const monthStart = toISODate(month)
  const monthEnd = toISODate(new Date(month.getFullYear(), month.getMonth() + 1, 0))
  const { data: plans } = useDatePlans(monthStart, monthEnd)
  const { data: encounters } = useEncounters(monthStart, monthEnd)
  const { data: upcoming } = useUpcomingDatePlans()
  const deletePlan = useDeleteDatePlan()
  const deleteEncounter = useDeleteEncounter()

  const plansByDate = useMemo(() => {
    const map = new Map<string, NonNullable<typeof plans>>()
    plans?.forEach((plan) => map.set(plan.date, [...(map.get(plan.date) ?? []), plan]))
    return map
  }, [plans])

  const encountersByDate = useMemo(() => {
    const map = new Map<string, NonNullable<typeof encounters>>()
    encounters?.forEach((e) => map.set(e.date, [...(map.get(e.date) ?? []), e]))
    return map
  }, [encounters])

  if (isLoading) return <PageLoader />

  const active = (partners ?? []).filter((p) => p.relationship?.status !== 'ENDED')

  if (active.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-surface">
        <EmptyState
          icon={<CalendarDays size={28} strokeWidth={1.5} />}
          title={t('agenda.title')}
          body={t('agenda.form.noPartners')}
          action={
            <Link
              to="/partners"
              className="inline-flex h-9 items-center rounded-md bg-peach px-3.5 text-[13.5px] font-medium text-on-peach hover:bg-peach-strong"
            >
              {t('partner.add')}
            </Link>
          }
        />
      </div>
    )
  }

  const today = todayISO()
  const firstWeekday = (month.getDay() + 6) % 7 // Monday-first
  const daysInMonth = new Date(month.getFullYear(), month.getMonth() + 1, 0).getDate()
  const cells: (string | null)[] = [
    ...Array.from({ length: firstWeekday }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) =>
      toISODate(new Date(month.getFullYear(), month.getMonth(), i + 1)),
    ),
  ]

  const selectedPlans = plansByDate.get(selected) ?? []
  const selectedEncounters = encountersByDate.get(selected) ?? []

  return (
    <div className="space-y-5">
      <header className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="font-display text-[21px] font-semibold text-ink">{t('agenda.title')}</h1>
          <p className="text-[12.5px] text-ink-3">{t('agenda.subtitle')}</p>
        </div>
        <div className="flex gap-2">
          <Button
            size="sm"
            variant="secondary"
            icon={<Flame size={13} />}
            onClick={() => setLogOpen(true)}
          >
            {t('agenda.logEncounter')}
          </Button>
          <Button size="sm" icon={<CalendarPlus size={13} />} onClick={() => setPlanOpen(true)}>
            {t('agenda.schedulePlan')}
          </Button>
        </div>
      </header>

      <div className="grid gap-5 lg:grid-cols-[1fr_280px]">
        <div className="space-y-5">
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
                    className={`relative mx-auto flex h-10 w-10 flex-col items-center justify-center rounded-md text-[13px] transition-colors hover:bg-surface-2 sm:h-11 sm:w-11 ${
                      selected === iso ? 'outline outline-2 outline-peach' : ''
                    } ${iso === today ? 'font-bold' : ''}`}
                  >
                    <span className={iso === today ? 'text-peach' : ''}>
                      {parseISODate(iso).getDate()}
                    </span>
                    <span aria-hidden="true" className="absolute bottom-1 flex gap-0.5">
                      {plansByDate.has(iso) && (
                        <span className="h-1 w-1 rounded-full bg-plum" />
                      )}
                      {encountersByDate.has(iso) && (
                        <span className="h-1 w-1 rounded-full bg-peach" />
                      )}
                    </span>
                  </button>
                ),
              )}
            </div>

            {/* Legend */}
            <div className="mt-4 flex flex-wrap gap-x-4 gap-y-1.5 border-t border-border pt-3 text-[11.5px] text-ink-2">
              <span className="flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 rounded-full bg-plum" />
                {t('agenda.legend.plan')}
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 rounded-full bg-peach" />
                {t('agenda.legend.encounter')}
              </span>
            </div>
          </div>

          {/* Upcoming dates */}
          <section className="rounded-xl border border-border bg-surface p-4">
            <h2 className="text-[12px] font-medium uppercase tracking-wide text-ink-3">
              {t('agenda.upcoming')}
            </h2>
            {(upcoming?.length ?? 0) > 0 ? (
              <ul className="mt-2 divide-y divide-border">
                {upcoming!.map((plan) => (
                  <li key={plan.id} className="flex items-baseline gap-3 py-2 text-[13px]">
                    <span className="w-24 shrink-0 font-medium text-peach">
                      {formatDate(plan.date)}
                    </span>
                    <span className="min-w-0 flex-1 truncate text-ink">
                      {plan.title}
                      {plan.partnerName && (
                        <span className="text-ink-3"> · {plan.partnerName}</span>
                      )}
                    </span>
                    {shortTime(plan.startTime) && (
                      <span className="text-ink-3">{shortTime(plan.startTime)}</span>
                    )}
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-2 text-[13px] text-ink-3">{t('agenda.noUpcoming')}</p>
            )}
          </section>

          {/* Cycle calendars of each partner */}
          <section className="rounded-xl border border-border bg-surface p-4">
            <h2 className="text-[12px] font-medium uppercase tracking-wide text-ink-3">
              {t('agenda.cycleCalendars')}
            </h2>
            <ul className="mt-2 flex flex-wrap gap-2">
              {active.map((partner) => (
                <li key={partner.id}>
                  <Link
                    to={`/partners/${partner.id}/calendar`}
                    className="flex items-center gap-2 rounded-md border border-border px-2.5 py-1.5 text-[13px] font-medium text-ink hover:bg-surface-2"
                  >
                    <Avatar emoji={partner.avatarEmoji} name={partner.name} size="sm" />
                    {partner.nickname ?? partner.name}
                    <ChevronRight size={13} className="text-ink-3" aria-hidden="true" />
                  </Link>
                </li>
              ))}
            </ul>
          </section>
        </div>

        {/* Selected day detail */}
        <aside className="h-fit rounded-xl border border-border bg-surface p-4">
          <h2 className="text-[14px] font-semibold text-ink">{formatDate(selected)}</h2>

          <div className="mt-3">
            <h3 className="text-[12px] font-medium uppercase tracking-wide text-ink-3">
              {t('agenda.plans')}
            </h3>
            {selectedPlans.length > 0 ? (
              <ul className="mt-1.5 space-y-2">
                {selectedPlans.map((plan) => (
                  <li key={plan.id} className="flex items-start gap-2 text-[13px]">
                    <div className="min-w-0 flex-1">
                      <p className="font-medium text-ink">
                        {plan.title}
                        {plan.partnerName && (
                          <span className="font-normal text-ink-3"> · {plan.partnerName}</span>
                        )}
                      </p>
                      <p className="flex flex-wrap items-center gap-x-3 text-[12px] text-ink-2">
                        {shortTime(plan.startTime) && (
                          <span className="inline-flex items-center gap-1">
                            <Clock3 size={11} aria-hidden="true" />
                            {shortTime(plan.startTime)}
                          </span>
                        )}
                        {plan.location && (
                          <span className="inline-flex items-center gap-1">
                            <MapPin size={11} aria-hidden="true" />
                            {plan.location}
                          </span>
                        )}
                      </p>
                      {plan.notes && <p className="text-[12px] text-ink-3">{plan.notes}</p>}
                    </div>
                    <button
                      onClick={() => setDeletePlanId(plan.id)}
                      aria-label={t('common.delete')}
                      className="rounded-md p-1 text-ink-3 hover:bg-surface-2 hover:text-danger"
                    >
                      <Trash2 size={13} />
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-1.5 text-[13px] text-ink-3">{t('agenda.nothingThatDay')}</p>
            )}
          </div>

          <div className="mt-4 border-t border-border pt-3">
            <h3 className="text-[12px] font-medium uppercase tracking-wide text-ink-3">
              {t('agenda.encounters')}
            </h3>
            {selectedEncounters.length > 0 ? (
              <ul className="mt-1.5 space-y-2">
                {selectedEncounters.map((encounter) => (
                  <li key={encounter.id} className="flex items-start gap-2 text-[13px]">
                    <Flame size={13} className="mt-0.5 shrink-0 text-peach" aria-hidden="true" />
                    <div className="min-w-0 flex-1">
                      <p className="text-ink">
                        {t('agenda.encounterWith', {
                          name: encounter.partnerName ?? t('agenda.unknownPartner'),
                        })}
                      </p>
                      {encounter.notes && (
                        <p className="text-[12px] text-ink-3">{encounter.notes}</p>
                      )}
                    </div>
                    <button
                      onClick={() => setDeleteEncounterId(encounter.id)}
                      aria-label={t('common.delete')}
                      className="rounded-md p-1 text-ink-3 hover:bg-surface-2 hover:text-danger"
                    >
                      <Trash2 size={13} />
                    </button>
                  </li>
                ))}
              </ul>
            ) : (
              <p className="mt-1.5 text-[13px] text-ink-3">{t('agenda.nothingThatDay')}</p>
            )}
          </div>

          <div className="mt-4 space-y-1.5 border-t border-border pt-3">
            <button
              onClick={() => setPlanOpen(true)}
              className="flex items-center gap-1.5 text-[13px] font-medium text-plum hover:underline"
            >
              <CalendarPlus size={13} aria-hidden="true" />
              {t('agenda.scheduleHere')}
            </button>
            {selected <= today && (
              <button
                onClick={() => setLogOpen(true)}
                className="flex items-center gap-1.5 text-[13px] font-medium text-peach hover:underline"
              >
                <Flame size={13} aria-hidden="true" />
                {t('agenda.logHere')}
              </button>
            )}
          </div>
        </aside>
      </div>

      <SchedulePlanSheet
        open={planOpen}
        onClose={() => setPlanOpen(false)}
        partners={active}
        initialDate={selected}
      />
      <LogEncounterSheet
        open={logOpen}
        onClose={() => setLogOpen(false)}
        partners={active}
        initialDate={selected}
      />

      <ConfirmDialog
        open={deletePlanId !== null}
        title={t('agenda.deletePlanConfirm')}
        body={t('common.irreversible')}
        danger
        busy={deletePlan.isPending}
        onConfirm={() =>
          deletePlan.mutate(deletePlanId!, {
            onSuccess: () => {
              toast(t('common.saved'))
              setDeletePlanId(null)
            },
          })
        }
        onClose={() => setDeletePlanId(null)}
      />
      <ConfirmDialog
        open={deleteEncounterId !== null}
        title={t('agenda.deleteEncounterConfirm')}
        body={t('common.irreversible')}
        danger
        busy={deleteEncounter.isPending}
        onConfirm={() =>
          deleteEncounter.mutate(deleteEncounterId!, {
            onSuccess: () => {
              toast(t('common.saved'))
              setDeleteEncounterId(null)
            },
          })
        }
        onClose={() => setDeleteEncounterId(null)}
      />
    </div>
  )
}
