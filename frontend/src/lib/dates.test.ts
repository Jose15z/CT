import { describe, expect, it } from 'vitest'
import { parseISODate, toISODate } from './dates'

describe('parseISODate', () => {
  it('parses as a local date, not UTC', () => {
    const date = parseISODate('2026-02-14')
    expect(date.getFullYear()).toBe(2026)
    expect(date.getMonth()).toBe(1)
    expect(date.getDate()).toBe(14)
  })

  it('round-trips with toISODate', () => {
    expect(toISODate(parseISODate('2026-09-21'))).toBe('2026-09-21')
    expect(toISODate(parseISODate('2024-02-29'))).toBe('2024-02-29')
  })

  it('pads months and days', () => {
    expect(toISODate(new Date(2026, 0, 5))).toBe('2026-01-05')
  })
})
