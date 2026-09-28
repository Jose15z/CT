import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'

interface PageHeaderProps {
  title: string
  description?: string
  /** Small uppercase label above the title (section name, partner name…). */
  eyebrow?: string
  /** Right-aligned primary/secondary actions. */
  actions?: ReactNode
  back?: { to: string; label: string }
}

/** One header pattern for every page: title, muted description, actions, hairline. */
export function PageHeader({ title, description, eyebrow, actions, back }: PageHeaderProps) {
  return (
    <div className="border-b border-border pb-5">
      {back && (
        <Link
          to={back.to}
          className="mb-3 inline-flex items-center gap-1.5 text-[12.5px] font-medium text-ink-3 transition-colors hover:text-ink"
        >
          <ArrowLeft size={13} aria-hidden="true" />
          {back.label}
        </Link>
      )}
      <div className="flex flex-wrap items-end justify-between gap-x-6 gap-y-3">
        <div className="min-w-0">
          {eyebrow && (
            <p className="mb-1 text-[11px] font-medium uppercase tracking-wider text-ink-3">
              {eyebrow}
            </p>
          )}
          <h1 className="text-[20px] font-semibold tracking-tight text-ink">{title}</h1>
          {description && <p className="mt-1 text-[13px] text-ink-3">{description}</p>}
        </div>
        {actions && <div className="flex shrink-0 items-center gap-2">{actions}</div>}
      </div>
    </div>
  )
}
