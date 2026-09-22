import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { KeyRound } from 'lucide-react'
import { AuthLayout } from './AuthLayout'
import { Field } from '../../components/ui/Field'
import { Input } from '../../components/ui/Input'
import { Button } from '../../components/ui/Button'
import { api } from '../../lib/api'
import { errorMessage } from '../../lib/errors'

export function ResetPasswordPage() {
  const { t } = useTranslation()
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''

  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const mismatch = confirm.length > 0 && password !== confirm

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await api<void>('/api/auth/reset', {
        method: 'POST',
        body: { token, newPassword: password },
        auth: false,
      })
      setDone(true)
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  if (!token) {
    return (
      <AuthLayout title={t('auth.resetTitle')}>
        <p className="text-[13.5px] leading-relaxed text-ink-2">{t('auth.resetMissingToken')}</p>
        <p className="mt-5 text-[13.5px] text-ink-2">
          <Link to="/forgot-password" className="font-medium text-peach hover:underline">
            {t('auth.forgotTitle')}
          </Link>
        </p>
      </AuthLayout>
    )
  }

  return (
    <AuthLayout title={t('auth.resetTitle')}>
      {done ? (
        <div className="space-y-4">
          <div className="flex items-start gap-3 rounded-md border border-border bg-surface p-4">
            <KeyRound size={18} className="mt-0.5 shrink-0 text-peach" aria-hidden="true" />
            <p className="text-[13.5px] leading-relaxed text-ink-2">{t('auth.resetDone')}</p>
          </div>
          <Link
            to="/login"
            className="inline-flex h-9 items-center rounded-md bg-peach px-3.5 text-[13.5px] font-medium text-on-peach hover:bg-peach-strong"
          >
            {t('auth.loginAction')}
          </Link>
        </div>
      ) : (
        <form onSubmit={submit} className="space-y-4" noValidate>
          <Field label={t('auth.newPassword')} htmlFor="reset-password">
            <Input
              id="reset-password"
              type="password"
              autoComplete="new-password"
              minLength={8}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
            <p className="mt-1 text-[11.5px] text-ink-3">{t('auth.passwordHint')}</p>
          </Field>
          <Field
            label={t('auth.confirmPassword')}
            htmlFor="reset-confirm"
            error={mismatch ? t('auth.passwordMismatch') : (error ?? undefined)}
          >
            <Input
              id="reset-confirm"
              type="password"
              autoComplete="new-password"
              value={confirm}
              onChange={(e) => setConfirm(e.target.value)}
              required
            />
          </Field>
          <Button
            type="submit"
            disabled={busy || password.length < 8 || password !== confirm}
            className="w-full"
          >
            {busy ? t('common.saving') : t('auth.resetAction')}
          </Button>
        </form>
      )}
    </AuthLayout>
  )
}
