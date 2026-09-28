import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Check, ChevronRight, X } from 'lucide-react'
import { useCheckIns, usePartners } from '../../lib/queries'
import type { Dashboard } from '../../lib/types'

const DISMISS_KEY = 'ct.onboardingDismissed'

function readDismissed(): boolean {
  try {
    return localStorage.getItem(DISMISS_KEY) === '1'
  } catch {
    return false
  }
}

interface Step {
  key: 'addPartner' | 'trackCycle' | 'firstCheckIn' | 'invitePartner'
  done: boolean
  to: string
}

/** First-run checklist: computed from real data, dismissable, gone once complete. */
export function OnboardingCard({ dashboard }: { dashboard: Dashboard }) {
  const { t } = useTranslation()
  const [dismissed, setDismissed] = useState(readDismissed)
  const { data: partners } = usePartners()
  const first = dashboard.partners[0]
  const { data: checkIns } = useCheckIns(first?.partnerId)

  const steps: Step[] = [
    { key: 'addPartner', done: dashboard.partners.length > 0, to: '/partners' },
    {
      key: 'trackCycle',
      done: dashboard.partners.some((p) => p.cycle.trackingEnabled && !p.cycle.insufficientData),
      to: first ? `/partners/${first.partnerId}` : '/partners',
    },
    {
      key: 'firstCheckIn',
      done: dashboard.partners.some((p) => p.myCheckInToday) || (checkIns?.length ?? 0) > 0,
      to: '/check-in',
    },
    {
      key: 'invitePartner',
      done: (partners ?? []).some((p) => p.linked),
      to: first ? `/partners/${first.partnerId}` : '/partners',
    },
  ]
  const doneCount = steps.filter((s) => s.done).length
  if (dismissed || doneCount === steps.length) return null

  const dismiss = () => {
    try {
      localStorage.setItem(DISMISS_KEY, '1')
    } catch {
      // ignore
    }
    setDismissed(true)
  }

  return (
    <section className="card overflow-hidden">
      <header className="flex items-center justify-between gap-3 border-b border-border px-5 py-3">
        <div className="min-w-0">
          <h2 className="text-[13px] font-semibold text-ink">{t('onboarding.title')}</h2>
          <p className="text-[12px] text-ink-3">
            {t('onboarding.progress', { done: doneCount, total: steps.length })}
          </p>
        </div>
        <button
          onClick={dismiss}
          aria-label={t('onboarding.dismiss')}
          title={t('onboarding.dismiss')}
          className="rounded-md p-1.5 text-ink-3 transition-colors hover:bg-surface-2 hover:text-ink"
        >
          <X size={14} />
        </button>
      </header>
      <div className="h-1 bg-surface-2">
        <div
          className="h-full bg-peach transition-[width]"
          style={{ width: `${(doneCount / steps.length) * 100}%` }}
        />
      </div>
      <ol className="divide-y divide-border">
        {steps.map((step) => (
          <li key={step.key}>
            <Link
              to={step.to}
              className={`flex items-center gap-3 px-5 py-2.5 text-[13.5px] transition-colors hover:bg-surface-2 ${
                step.done ? 'text-ink-3' : 'text-ink'
              }`}
            >
              <span
                aria-hidden="true"
                className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full border ${
                  step.done ? 'border-teal bg-teal text-white' : 'border-border-strong'
                }`}
              >
                {step.done && <Check size={11} strokeWidth={3} />}
              </span>
              <span className={`min-w-0 flex-1 ${step.done ? 'line-through' : 'font-medium'}`}>
                {t(`onboarding.steps.${step.key}`)}
              </span>
              {!step.done && <ChevronRight size={14} className="text-ink-3" aria-hidden="true" />}
            </Link>
          </li>
        ))}
      </ol>
    </section>
  )
}
