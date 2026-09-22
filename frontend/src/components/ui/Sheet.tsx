import { useEffect } from 'react'
import type { ReactNode } from 'react'
import { createPortal } from 'react-dom'
import { X } from 'lucide-react'
import { useTranslation } from 'react-i18next'

interface SheetProps {
  open: boolean
  onClose: () => void
  title: string
  children: ReactNode
}

/** Bottom sheet on mobile, centered dialog on larger screens. */
export function Sheet({ open, onClose, title, children }: SheetProps) {
  const { t } = useTranslation()

  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onClose()
    }
    document.addEventListener('keydown', onKey)
    document.body.style.overflow = 'hidden'
    return () => {
      document.removeEventListener('keydown', onKey)
      document.body.style.overflow = ''
    }
  }, [open, onClose])

  if (!open) return null

  return createPortal(
    <div className="fixed inset-0 z-50 flex items-end justify-center sm:items-center">
      <div
        className="absolute inset-0 bg-black/40"
        onClick={onClose}
        aria-hidden="true"
      />
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className="relative z-10 max-h-[88vh] w-full overflow-y-auto rounded-t-xl border border-border bg-surface p-5 shadow-lg sm:max-w-lg sm:rounded-xl"
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-[16px] font-semibold text-ink">{title}</h2>
          <button
            onClick={onClose}
            aria-label={t('common.close')}
            className="rounded-md p-1.5 text-ink-3 hover:bg-surface-2 hover:text-ink"
          >
            <X size={17} />
          </button>
        </div>
        {children}
      </div>
    </div>,
    document.body,
  )
}
