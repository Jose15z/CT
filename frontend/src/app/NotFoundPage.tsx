import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'

export function NotFoundPage() {
  const { t } = useTranslation()
  return (
    <div className="flex min-h-dvh flex-col items-center justify-center gap-3 px-6 text-center">
      <span className="text-4xl" aria-hidden="true">🍑</span>
      <p className="text-[15px] text-ink-2">{t('common.notFound')}</p>
      <Link to="/dashboard" className="text-[14px] font-medium text-peach hover:underline">
        {t('common.goHome')}
      </Link>
    </div>
  )
}
