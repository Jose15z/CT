import { useEffect, useRef, useState } from 'react'
import type { MouseEvent } from 'react'

/** Container width via ResizeObserver so SVG text stays crisp at any size. */
function useWidth() {
  const ref = useRef<HTMLDivElement>(null)
  const [width, setWidth] = useState(0)
  useEffect(() => {
    const el = ref.current
    if (!el) return
    setWidth(el.getBoundingClientRect().width)
    const observer = new ResizeObserver((entries) => setWidth(entries[0].contentRect.width))
    observer.observe(el)
    return () => observer.disconnect()
  }, [])
  return [ref, width] as const
}

export interface Series {
  key: string
  label: string
  /** A CSS color; charts use dedicated, validated tokens. */
  color: string
  values: (number | null)[]
}

interface LineChartProps {
  dates: string[]
  series: Series[]
  min?: number
  max?: number
  height?: number
  formatDate: (iso: string) => string
  ariaLabel: string
}

/**
 * Small-multiple line chart: one axis, 2px lines, surface-ringed markers,
 * recessive grid, crosshair + tooltip on hover. Gaps (null) break the line
 * instead of interpolating a value nobody recorded.
 */
export function LineChart({
  dates,
  series,
  min = 1,
  max = 5,
  height = 180,
  formatDate,
  ariaLabel,
}: LineChartProps) {
  const [ref, width] = useWidth()
  const [hover, setHover] = useState<number | null>(null)
  const pad = { top: 12, right: 12, bottom: 24, left: 26 }
  const w = Math.max(width, 240)
  const innerW = w - pad.left - pad.right
  const innerH = height - pad.top - pad.bottom
  const n = dates.length
  const x = (i: number) => pad.left + (n <= 1 ? innerW / 2 : (i / (n - 1)) * innerW)
  const y = (v: number) => pad.top + innerH - ((v - min) / (max - min)) * innerH

  const pathFor = (values: (number | null)[]) => {
    let d = ''
    let open = false
    values.forEach((v, i) => {
      if (v === null) {
        open = false
        return
      }
      d += `${open ? 'L' : 'M'}${x(i).toFixed(1)} ${y(v).toFixed(1)} `
      open = true
    })
    return d
  }

  const onMove = (e: MouseEvent<SVGSVGElement>) => {
    const rect = e.currentTarget.getBoundingClientRect()
    const px = e.clientX - rect.left
    const idx = Math.round(((px - pad.left) / innerW) * (n - 1))
    setHover(Math.max(0, Math.min(n - 1, idx)))
  }

  const gridValues: number[] = []
  for (let v = min; v <= max; v++) gridValues.push(v)
  const xTicks = n > 1 ? [0, Math.floor((n - 1) / 2), n - 1] : [0]
  const tooltipLeft = hover === null ? 0 : Math.min(Math.max(x(hover) - 70, 0), w - 150)

  return (
    <div ref={ref} className="relative">
      <svg
        width={w}
        height={height}
        role="img"
        aria-label={ariaLabel}
        onMouseMove={onMove}
        onMouseLeave={() => setHover(null)}
        className="block"
      >
        {gridValues.map((v) => (
          <g key={v}>
            <line
              x1={pad.left}
              x2={w - pad.right}
              y1={y(v)}
              y2={y(v)}
              stroke="var(--border)"
              strokeWidth={1}
            />
            <text x={pad.left - 8} y={y(v) + 3.5} textAnchor="end" fontSize={10} fill="var(--ink-3)">
              {v}
            </text>
          </g>
        ))}
        {xTicks.map((i) => (
          <text
            key={i}
            x={x(i)}
            y={height - 7}
            textAnchor={i === 0 ? 'start' : i === n - 1 ? 'end' : 'middle'}
            fontSize={10}
            fill="var(--ink-3)"
          >
            {formatDate(dates[i])}
          </text>
        ))}
        {hover !== null && (
          <line
            x1={x(hover)}
            x2={x(hover)}
            y1={pad.top}
            y2={pad.top + innerH}
            stroke="var(--border-strong)"
            strokeWidth={1}
          />
        )}
        {series.map((s) => (
          <g key={s.key}>
            <path
              d={pathFor(s.values)}
              fill="none"
              stroke={s.color}
              strokeWidth={2}
              strokeLinejoin="round"
              strokeLinecap="round"
            />
            {s.values.map((v, i) =>
              v === null ? null : (
                <circle
                  key={i}
                  cx={x(i)}
                  cy={y(v)}
                  r={hover === i ? 5 : 3.5}
                  fill={s.color}
                  stroke="var(--surface)"
                  strokeWidth={2}
                />
              ),
            )}
          </g>
        ))}
      </svg>
      {hover !== null && (
        <div
          className="pointer-events-none absolute top-0 w-[150px] rounded-lg border border-border bg-surface px-2.5 py-2 text-[11.5px] shadow-raised"
          style={{ left: tooltipLeft }}
        >
          <p className="font-medium text-ink">{formatDate(dates[hover])}</p>
          {series.map((s) => (
            <p key={s.key} className="mt-0.5 flex items-center gap-1.5 text-ink-2">
              <span className="h-2 w-2 rounded-full" style={{ background: s.color }} aria-hidden="true" />
              <span className="flex-1">{s.label}</span>
              <span className="font-medium tabular-nums text-ink">
                {s.values[hover] === null ? '—' : s.values[hover]}
              </span>
            </p>
          ))}
        </div>
      )}
    </div>
  )
}

interface BarChartProps {
  items: { label: string; value: number }[]
  color: string
  height?: number
  ariaLabel: string
}

/** Thin bars with rounded data-ends anchored to the baseline; every bar labeled. */
export function BarChart({ items, color, height = 160, ariaLabel }: BarChartProps) {
  const [ref, width] = useWidth()
  const [hover, setHover] = useState<number | null>(null)
  const pad = { top: 18, right: 8, bottom: 24, left: 8 }
  const w = Math.max(width, 240)
  const innerW = w - pad.left - pad.right
  const innerH = height - pad.top - pad.bottom
  const max = Math.max(1, ...items.map((i) => i.value))
  const slot = innerW / Math.max(items.length, 1)
  const barW = Math.min(28, slot * 0.5)

  return (
    <div ref={ref} className="relative">
      <svg width={w} height={height} role="img" aria-label={ariaLabel} className="block">
        <line
          x1={pad.left}
          x2={w - pad.right}
          y1={pad.top + innerH}
          y2={pad.top + innerH}
          stroke="var(--border)"
        />
        {items.map((item, i) => {
          const cx = pad.left + slot * i + slot / 2
          const h = (item.value / max) * innerH
          const top = pad.top + innerH - h
          const r = Math.min(4, h)
          const left = cx - barW / 2
          const d =
            h === 0
              ? ''
              : `M${left} ${pad.top + innerH} V${top + r} Q${left} ${top} ${left + r} ${top} H${left + barW - r} Q${left + barW} ${top} ${left + barW} ${top + r} V${pad.top + innerH} Z`
          return (
            <g
              key={item.label}
              onMouseEnter={() => setHover(i)}
              onMouseLeave={() => setHover(null)}
            >
              <rect x={cx - slot / 2} y={pad.top} width={slot} height={innerH} fill="transparent" />
              {h > 0 && <path d={d} fill={color} opacity={hover === null || hover === i ? 1 : 0.55} />}
              <text
                x={cx}
                y={top - 6}
                textAnchor="middle"
                fontSize={11}
                fill="var(--ink-2)"
                className="tabular-nums"
              >
                {item.value}
              </text>
              <text x={cx} y={height - 7} textAnchor="middle" fontSize={10} fill="var(--ink-3)">
                {item.label}
              </text>
            </g>
          )
        })}
      </svg>
    </div>
  )
}
