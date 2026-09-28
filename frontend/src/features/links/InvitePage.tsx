import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Link2, ShieldCheck } from 'lucide-react'
import { AuthLayout } from '../auth/AuthLayout'
import { Button } from '../../components/ui/Button'
import { PageLoader } from '../../components/ui/Spinner'
import { useAuth } from '../../lib/auth'
import { useAcceptInvite, useInvitePreview } from '../../lib/queries'
import { errorMessage } from '../../lib/errors'
import { formatDate } from '../../lib/dates'

export const PENDING_INVITE_KEY = 'ct.pendingInvite'

export function readPendingInvite(): string | null {
  try {
    return sessionStorage.getItem(PENDING_INVITE_KEY)
  } catch {
    return null
  }
}

export function clearPendingInvite() {
  try {
    sessionStorage.removeItem(PENDING_INVITE_KEY)
  } catch {
    // ignore
  }
}

/**
 * /invite/:token — public landing for an invite link. Logged-out visitors see
 * the preview and are sent to register/login; the token survives that trip
 * in sessionStorage so they land back here to accept.
 */
export function InvitePage() {
  const { token } = useParams<{ token: string }>()
  const { t } = useTranslation()
  const navigate = useNavigate()
  const { user, initializing } = useAuth()
  const { data: preview, isLoading, isError } = useInvitePreview(token)
  const accept = useAcceptInvite()
  const [error, setError] = useState<string | null>(null)
  const [done, setDone] = useState(false)

  useEffect(() => {
    if (!token) return
    try {
      sessionStorage.setItem(PENDING_INVITE_KEY, token)
    } catch {
      // ignore
    }
  }, [token])

  if (isLoading || initializing) {
    return (
      <AuthLayout title={t('invite.title')}>
        <PageLoader />
      </AuthLayout>
    )
  }

  if (isError || !preview || !token) {
    return (
      <AuthLayout title={t('invite.title')}>
        <p className="text-[13.5px] leading-relaxed text-ink-2">{t('invite.invalid')}</p>
        <p className="mt-5 text-[13.5px]">
          <Link to="/" className="font-medium text-peach hover:underline">
            {t('common.goHome')}
          </Link>
        </p>
      </AuthLayout>
    )
  }

  const submit = () => {
    setError(null)
    accept.mutate(token, {
      onSuccess: () => {
        clearPendingInvite()
        setDone(true)
      },
      onError: (err) => setError(errorMessage(err)),
    })
  }

  return (
    <AuthLayout title={t('invite.title')}>
      <div className="card p-5">
        <div className="flex h-9 w-9 items-center justify-center rounded-lg bg-peach-soft text-peach">
          <Link2 size={17} aria-hidden="true" />
        </div>
        <p className="mt-3 text-[15px] font-semibold tracking-tight text-ink">
          {t('invite.headline', { inviter: preview.inviterDisplayName, name: preview.partnerName })}
        </p>
        {preview.relationshipType && (
          <p className="mt-1 text-[13px] text-ink-3">
            {t(`relationshipType.${preview.relationshipType}`)}
          </p>
        )}
        <div className="mt-4 flex items-start gap-2.5 rounded-lg border border-border bg-surface-2/60 px-3.5 py-3">
          <ShieldCheck size={15} className="mt-0.5 shrink-0 text-teal" aria-hidden="true" />
          <p className="text-[12.5px] leading-relaxed text-ink-2">{t('invite.consentNote')}</p>
        </div>
        <p className="mt-3 text-[11.5px] text-ink-3">
          {t('invite.expires', { date: formatDate(preview.expiresAt.slice(0, 10)) })}
        </p>

        {done ? (
          <div className="mt-5 space-y-3">
            <p className="text-[13.5px] font-medium text-success">{t('invite.done')}</p>
            <Button onClick={() => navigate('/dashboard')}>{t('common.goHome')}</Button>
          </div>
        ) : user ? (
          <div className="mt-5 space-y-3">
            <p className="text-[13px] text-ink-2">
              {t('invite.acceptingAs', { name: user.displayName })}
            </p>
            {error && <p className="text-[13px] text-danger">{error}</p>}
            <div className="flex flex-wrap gap-2">
              <Button onClick={submit} disabled={accept.isPending}>
                {accept.isPending ? t('common.saving') : t('invite.accept')}
              </Button>
              <Button variant="secondary" onClick={() => navigate('/dashboard')}>
                {t('common.cancel')}
              </Button>
            </div>
          </div>
        ) : (
          <div className="mt-5 space-y-3">
            <p className="text-[13px] text-ink-2">{t('invite.needAccount')}</p>
            <div className="flex flex-wrap gap-2">
              <Button onClick={() => navigate('/register')}>{t('auth.registerAction')}</Button>
              <Button variant="secondary" onClick={() => navigate('/login')}>
                {t('auth.loginAction')}
              </Button>
            </div>
          </div>
        )}
      </div>
    </AuthLayout>
  )
}
