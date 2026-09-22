import i18n from '../i18n'
import type { Duration } from './types'

function locale(): string {
  return i18n.language?.startsWith('en') ? 'en-US' : 'es-ES'
}

/** Parse an ISO date (yyyy-mm-dd) as a LOCAL date, not UTC. */
export function parseISODate(iso: string): Date {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(year, month - 1, day)
}

export function toISODate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function todayISO(): string {
  return toISODate(new Date())
}

/** "14 feb 2024" / "Feb 14, 2024" */
export function formatDate(iso: string): string {
  return parseISODate(iso).toLocaleDateString(locale(), {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
  })
}

/** "14 de febrero" / "February 14" */
export function formatDayMonth(iso: string): string {
  return parseISODate(iso).toLocaleDateString(locale(), { day: 'numeric', month: 'long' })
}

/** Spanish month/weekday names are lowercase; only the first letter is raised. */
export function capitalizeFirst(text: string): string {
  return text.charAt(0).toUpperCase() + text.slice(1)
}

/** "Febrero 2026" / "February 2026" */
export function formatMonthYear(date: Date): string {
  return capitalizeFirst(date.toLocaleDateString(locale(), { month: 'long', year: 'numeric' }))
}

/** "Lunes, 21 de septiembre" / "Monday, September 21" */
export function formatFullDay(date: Date): string {
  return capitalizeFirst(
    date.toLocaleDateString(locale(), { weekday: 'long', day: 'numeric', month: 'long' }),
  )
}

export function weekdayInitials(): string[] {
  // Monday-first week, matching the calendar grid.
  const base = new Date(2024, 0, 1) // a Monday
  return Array.from({ length: 7 }, (_, i) => {
    const d = new Date(base)
    d.setDate(base.getDate() + i)
    return d.toLocaleDateString(locale(), { weekday: 'narrow' })
  })
}

/**
 * "2 años y 7 meses" / "2 years and 7 months". Falls back to days for very
 * fresh relationships.
 */
export function formatDuration(duration: Duration): string {
  const { years, months, days } = duration
  if (years === 0 && months === 0) {
    return i18n.t('common.day', { count: days })
  }
  if (years === 0) {
    return i18n.t('common.month', { count: months })
  }
  if (months === 0) {
    return i18n.t('common.year', { count: years })
  }
  return i18n.t('duration.full', {
    years: i18n.t('common.year', { count: years }),
    months: i18n.t('common.month', { count: months }),
  })
}
