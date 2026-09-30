import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { CalendarDays, HeartHandshake, MessagesSquare, Trophy } from 'lucide-react'
import { PublicHeader } from '../../app/PublicLayout'
import { usePageMeta } from '../../lib/seo'

const features = [
  { key: 'cycle', icon: CalendarDays },
  { key: 'relationship', icon: HeartHandshake },
  { key: 'checkins', icon: MessagesSquare },
  { key: 'ranking', icon: Trophy },
] as const

export function LandingPage() {
  const { t } = useTranslation()
  usePageMeta({ title: t('seo.home.title'), description: t('seo.home.description'), index: true })

  return (
    <div className="min-h-dvh">
      <PublicHeader />

      <main className="mx-auto max-w-3xl px-5 pb-16">
        <section className="pt-10 sm:pt-16">
          <h1 className="text-[30px] font-semibold leading-[1.15] tracking-[-0.02em] text-ink sm:text-[38px]">
            {t('app.tagline')}
          </h1>
          <p className="mt-3 max-w-xl text-[14.5px] leading-relaxed text-ink-2">
            {t('landing.intro')}
          </p>
          <div className="mt-6 flex flex-wrap items-center gap-3">
            <Link
              to="/register"
              className="inline-flex h-10 items-center rounded-md bg-peach px-4 text-[14px] font-medium text-on-peach hover:bg-peach-strong"
            >
              {t('landing.cta')}
            </Link>
            <Link
              to="/login"
              className="inline-flex h-10 items-center rounded-md border border-border-strong px-4 text-[14px] font-medium text-ink hover:bg-surface-2"
            >
              {t('landing.login')}
            </Link>
          </div>
        </section>

        <section className="mt-12 grid gap-3 sm:grid-cols-2">
          {features.map(({ key, icon: Icon }) => (
            <div key={key} className="card p-5">
              <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-peach-soft text-peach">
                <Icon size={16} strokeWidth={1.9} aria-hidden="true" />
              </div>
              <h2 className="mt-3 text-[14px] font-semibold tracking-tight text-ink">
                {t(`landing.features.${key}.title`)}
              </h2>
              <p className="mt-1 text-[13.5px] leading-relaxed text-ink-2">
                {t(`landing.features.${key}.body`)}
              </p>
            </div>
          ))}
        </section>

        <p className="mt-10 border-t border-border pt-5 text-[12.5px] leading-relaxed text-ink-3">
          {t('landing.consent')}{' '}
          <Link to="/privacy" className="font-medium text-ink-2 hover:text-ink hover:underline">
            {t('privacy.title')}
          </Link>
        </p>
      </main>
    </div>
  )
}
