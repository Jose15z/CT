import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { MailCheck } from 'lucide-react'
import { AuthLayout } from './AuthLayout'
import { Field } from '../../components/ui/Field'
import { Input } from '../../components/ui/Input'
import { Button } from '../../components/ui/Button'
import { api } from '../../lib/api'
import { errorMessage } from '../../lib/errors'

export function ForgotPasswordPage() {
  const { t } = useTranslation()
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await api<void>('/api/auth/forgot', {
        method: 'POST',
        body: { email: email.trim() },
        auth: false,
      })
      setSent(true)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title={t('auth.forgotTitle')}>
      {sent ? (
        <div className="space-y-4">
          <div className="flex items-start gap-3 rounded-md border border-border bg-surface p-4">
            <MailCheck size={18} className="mt-0.5 shrink-0 text-peach" aria-hidden="true" />
            <p className="text-[13.5px] leading-relaxed text-ink-2">{t('auth.forgotSent')}</p>
          </div>
          <Link to="/login" className="text-[13.5px] font-medium text-peach hover:underline">
            {t('auth.backToLogin')}
          </Link>
        </div>
      ) : (
        <>
          <p className="text-[13.5px] leading-relaxed text-ink-2">{t('auth.forgotIntro')}</p>
          <form onSubmit={submit} className="mt-4 space-y-4" noValidate>
            <Field label={t('auth.email')} htmlFor="forgot-email" error={error ?? undefined}>
              <Input
                id="forgot-email"
                type="email"
                autoComplete="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />
            </Field>
            <Button type="submit" disabled={busy || !email.trim()} className="w-full">
              {busy ? t('common.saving') : t('auth.forgotAction')}
            </Button>
          </form>
          <p className="mt-5 text-[13.5px] text-ink-2">
            <Link to="/login" className="font-medium text-peach hover:underline">
              {t('auth.backToLogin')}
            </Link>
          </p>
        </>
      )}
    </AuthLayout>
  )
}
