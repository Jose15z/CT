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

export function LoginPage() {
  const { t } = useTranslation()
  const { login } = useAuth()
  const navigate = useNavigate()
  const [identifier, setIdentifier] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setError(null)
    setBusy(true)
    try {
      await login(identifier.trim(), password)
      navigate('/dashboard')
    } catch (err) {
      setError(errorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <AuthLayout title={t('auth.loginTitle')}>
      <form onSubmit={submit} className="space-y-4" noValidate>
        <Field label={t('auth.usernameOrEmail')} htmlFor="identifier">
          <Input
            id="identifier"
            autoComplete="username"
            value={identifier}
            onChange={(e) => setIdentifier(e.target.value)}
            required
          />
        </Field>
        <Field label={t('auth.password')} htmlFor="password" error={error ?? undefined}>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
          />
        </Field>
        <Button type="submit" disabled={busy || !identifier || !password} className="w-full">
          {busy ? t('auth.loggingIn') : t('auth.loginAction')}
        </Button>
      </form>
      <p className="mt-5 text-[13.5px] text-ink-2">
        {t('auth.noAccount')}{' '}
        <Link to="/register" className="font-medium text-peach hover:underline">
          {t('auth.registerLink')}
        </Link>
      </p>
    </AuthLayout>
  )
}
