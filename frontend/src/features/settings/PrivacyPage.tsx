import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft } from 'lucide-react'
import { useAuth } from '../../lib/auth'
import { usePageMeta } from '../../lib/seo'

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
  const { user } = useAuth()
  usePageMeta({
    title: t('seo.privacy.title'),
    description: t('seo.privacy.description'),
    index: true,
  })

  return (
    <div className="mx-auto max-w-xl space-y-5">
      <Link
        to={user ? '/settings' : '/'}
        className="inline-flex items-center gap-1.5 text-[13px] font-medium text-ink-3 hover:text-ink"
      >
        <ArrowLeft size={14} aria-hidden="true" />
        {user ? t('settings.title') : t('privacy.backHome')}
      </Link>

      <header>
        <h1 className="text-[20px] font-semibold tracking-tight text-ink">{t('privacy.title')}</h1>
        <p className="mt-1 text-[13.5px] leading-relaxed text-ink-2">{t('privacy.intro')}</p>
      </header>

      <div className="space-y-5 card p-4 sm:p-5">
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
