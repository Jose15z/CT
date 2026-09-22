import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { AuthLayout } from './AuthLayout'
import { Field } from '../../components/ui/Field'
import { Input } from '../../components/ui/Input'
import { Button } from '../../components/ui/Button'
import { useAuth } from '../../lib/auth'
import { errorMessage } from '../../lib/errors'

export function RegisterPage() {
  const { t, i18n } = useTranslation()
  const { register } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState({ username: '', email: '', password: '', displayName: '' })
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const set = (key: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((prev) => ({ ...prev, [key]: e.target.value }))

  const valid =
    form.username.length >= 3 &&
    form.email.includes('@') &&
    form.password.length >= 8 &&
    form.displayName.trim().length > 0

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await register({
        ...form,
        displayName: form.displayName.trim(),
        preferredLanguage: i18n.language?.startsWith('en') ? 'en' : 'es',
      })
      navigate('/dashboard')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title={t('auth.registerTitle')}>
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label={t('auth.displayName')} htmlFor="displayName" hint={t('auth.displayNameHint')}>
          <Input id="displayName" value={form.displayName} onChange={set('displayName')} required />
        </Field>
        <Field label={t('auth.username')} htmlFor="username">
          <Input
            id="username"
            autoComplete="username"
            value={form.username}
            onChange={set('username')}
            required
          />
        </Field>
        <Field label={t('auth.email')} htmlFor="email">
          <Input
            id="email"
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={set('email')}
            required
          />
        </Field>
        <Field
          label={t('auth.password')}
          htmlFor="password"
          hint={t('auth.passwordHint')}
          error={error ?? undefined}
        >
          <Input
            id="password"
            type="password"
            autoComplete="new-password"
            value={form.password}
            onChange={set('password')}
            required
          />
        </Field>
        <Button type="submit" disabled={busy || !valid} className="w-full">
          {busy ? t('common.saving') : t('auth.registerAction')}
        </Button>
      </form>
      <p className="mt-5 text-[13.5px] text-ink-2">
        {t('auth.hasAccount')}{' '}
        <Link to="/login" className="font-medium text-peach hover:underline">
          {t('auth.loginLink')}
        </Link>
      </p>
    </AuthLayout>
  )
}
