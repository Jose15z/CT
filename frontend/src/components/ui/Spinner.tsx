import { Loader2 } from 'lucide-react'
import { useTranslation } from 'react-i18next'

export function Spinner({ size = 18 }: { size?: number }) {
  return <Loader2 size={size} className="animate-spin text-ink-3" aria-hidden="true" />
}

export function PageLoader() {
  const { t } = useTranslation()
  return (
    <div className="flex items-center justify-center gap-2 py-20 text-[13.5px] text-ink-3">
      <Spinner />
      {t('common.loading')}
    </div>
  )
}
