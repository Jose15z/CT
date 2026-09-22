import type { ReactNode } from 'react'

type Tone = 'neutral' | 'peach' | 'plum' | 'rose' | 'teal' | 'danger'

const tones: Record<Tone, string> = {
  neutral: 'bg-surface-2 text-ink-2',
  peach: 'bg-peach-soft text-peach',
  plum: 'bg-plum-soft text-plum',
  rose: 'bg-rose-soft text-rose',
  teal: 'bg-teal-soft text-teal',
  danger: 'bg-danger-soft text-danger',
}

export function Tag({ tone = 'neutral', children }: { tone?: Tone; children: ReactNode }) {
  return (
    <span
      className={`inline-flex items-center gap-1 rounded px-1.5 py-0.5 text-[11.5px] font-medium ${tones[tone]}`}
    >
      {children}
    </span>
  )
}
