import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Check, Copy, Link2, Unlink } from 'lucide-react'
import { Button } from '../../components/ui/Button'
import { Sheet } from '../../components/ui/Sheet'
import { Switch } from '../../components/ui/Switch'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { useToast } from '../../components/ui/Toast'
import { useCreateInvite, useGrantsGiven, useSetGrant, useUnlink } from '../../lib/queries'
import { errorMessage } from '../../lib/errors'
import { formatDate } from '../../lib/dates'
import type { AccessScope, Partner } from '../../lib/types'

/** Owner-side controls: invite the real person, then choose what to share with them. */
export function LinkedAccountSection({ partner }: { partner: Partner }) {
  const { t } = useTranslation()
  const toast = useToast()
  const createInvite = useCreateInvite(partner.id)
  const { data: grants } = useGrantsGiven(partner.linked ? partner.id : undefined)
  const setGrant = useSetGrant(partner.id)
  const unlink = useUnlink()

  const [inviteOpen, setInviteOpen] = useState(false)
  const [copied, setCopied] = useState(false)
  const [confirmUnlink, setConfirmUnlink] = useState(false)

  const isActive = (scope: AccessScope) =>
    grants?.some((g) => g.scope === scope && g.status === 'ACTIVE') ?? false

  const toggle = (scope: AccessScope, enabled: boolean) =>
    setGrant.mutate(
      { scope, enabled },
      { onError: (err) => toast(errorMessage(err), 'error') },
    )

  const openInvite = () => {
    setCopied(false)
    createInvite.mutate(undefined, {
      onSuccess: () => setInviteOpen(true),
      onError: (err) => toast(errorMessage(err), 'error'),
    })
  }

  const copy = async () => {
    if (!createInvite.data) return
    try {
      await navigator.clipboard.writeText(createInvite.data.url)
      setCopied(true)
    } catch {
      toast(t('common.error'), 'error')
    }
  }

  return (
    <section>
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-[15px] font-semibold tracking-tight text-ink">
          {t('link.sectionTitle')}
        </h2>
        {partner.linked ? (
          <Button
            variant="ghost"
            size="sm"
            icon={<Unlink size={13} />}
            onClick={() => setConfirmUnlink(true)}
          >
            {t('link.unlink')}
          </Button>
        ) : (
          <Button
            variant="secondary"
            size="sm"
            icon={<Link2 size={13} />}
            onClick={openInvite}
            disabled={createInvite.isPending}
          >
            {createInvite.isPending ? t('common.loading') : t('link.invite', { name: partner.nickname ?? partner.name })}
          </Button>
        )}
      </div>

      {partner.linked ? (
        <div className="mt-2">
          <p className="text-[13px] text-ink-2">
            {t('link.linkedWith', { username: partner.linkedUsername ?? '' })}
          </p>
          <div className="mt-2 divide-y divide-border">
            <Switch
              label={t('link.shareCheckIns')}
              description={t('link.shareCheckInsHint')}
              checked={isActive('CHECK_INS')}
              disabled={setGrant.isPending}
              onChange={(enabled) => toggle('CHECK_INS', enabled)}
            />
            <Switch
              label={t('link.shareCycle')}
              description={t('link.shareCycleHint')}
              checked={isActive('CYCLE')}
              disabled={setGrant.isPending}
              onChange={(enabled) => toggle('CYCLE', enabled)}
            />
          </div>
        </div>
      ) : (
        <p className="mt-1 text-[13px] leading-relaxed text-ink-3">{t('link.notLinkedHint')}</p>
      )}

      <Sheet open={inviteOpen} onClose={() => setInviteOpen(false)} title={t('link.inviteTitle')}>
        <div className="space-y-4">
          <p className="text-[13.5px] leading-relaxed text-ink-2">{t('link.inviteBody')}</p>
          <div className="flex items-center gap-2 rounded-lg border border-border bg-surface-2/60 px-3 py-2">
            <code className="min-w-0 flex-1 truncate text-[12.5px] text-ink">
              {createInvite.data?.url}
            </code>
            <Button
              size="sm"
              variant="secondary"
              icon={copied ? <Check size={13} /> : <Copy size={13} />}
              onClick={copy}
            >
              {copied ? t('link.copied') : t('link.copy')}
            </Button>
          </div>
          {createInvite.data && (
            <p className="text-[12px] text-ink-3">
              {t('link.inviteExpires', { date: formatDate(createInvite.data.expiresAt.slice(0, 10)) })}
            </p>
          )}
          <div className="flex justify-end">
            <Button onClick={() => setInviteOpen(false)}>{t('common.close')}</Button>
          </div>
        </div>
      </Sheet>

      <ConfirmDialog
        open={confirmUnlink}
        title={t('link.unlinkConfirmTitle')}
        body={t('link.unlinkConfirmBody')}
        danger
        busy={unlink.isPending}
        onConfirm={() =>
          unlink.mutate(partner.id, {
            onSuccess: () => {
              toast(t('common.saved'))
              setConfirmUnlink(false)
            },
            onError: (err) => toast(errorMessage(err), 'error'),
          })
        }
        onClose={() => setConfirmUnlink(false)}
      />
    </section>
  )
}
