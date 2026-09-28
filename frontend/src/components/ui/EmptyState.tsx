import type { ReactNode } from 'react'

interface EmptyStateProps {
  icon: ReactNode
  title: string
  body: string
  action?: ReactNode
}

export function EmptyState({ icon, title, body, action }: EmptyStateProps) {
  return (
    <div className="flex flex-col items-center px-6 py-14 text-center">
      <div className="mb-4 flex h-11 w-11 items-center justify-center rounded-full border border-border bg-surface-2 text-ink-3">
        {icon}
      </div>
      <h3 className="text-[15px] font-semibold tracking-tight text-ink">{title}</h3>
      <p className="mt-1 max-w-sm text-[13.5px] leading-relaxed text-ink-2">{body}</p>
      {action && <div className="mt-5">{action}</div>}
    </div>
  )
}
