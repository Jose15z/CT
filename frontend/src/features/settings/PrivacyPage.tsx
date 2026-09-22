import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft } from 'lucide-react'

const SECTIONS = [
  'consent',
  'checkins',
  'observations',
  'estimates',
  'leaderboard',
  'authorization',
] as const

export function PrivacyPage() {
  const { t } = useTranslation()

  return (
    <div className="mx-auto max-w-xl space-y-5">
      <Link
        to="/settings"
        className="inline-flex items-center gap-1.5 text-[13px] font-medium text-ink-3 hover:text-ink"
      >
        <ArrowLeft size={14} aria-hidden="true" />
        {t('settings.title')}
      </Link>

      <header>
        <h1 className="font-display text-[21px] font-semibold text-ink">{t('privacy.title')}</h1>
        <p className="mt-1 text-[13.5px] leading-relaxed text-ink-2">{t('privacy.intro')}</p>
      </header>

      <div className="space-y-5 rounded-xl border border-border bg-surface p-4 sm:p-5">
        {SECTIONS.map((section, index) => (
          <section key={section} className={index > 0 ? 'border-t border-border pt-5' : ''}>
            <h2 className="text-[14.5px] font-semibold text-ink">
              {t(`privacy.sections.${section}.title`)}
            </h2>
            <p className="mt-1 text-[13.5px] leading-relaxed text-ink-2">
              {t(`privacy.sections.${section}.body`)}
            </p>
          </section>
        ))}
      </div>
    </div>
  )
}
