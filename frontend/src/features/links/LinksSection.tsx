import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Link2, SmilePlus, Unlink } from 'lucide-react'
import { Switch } from '../../components/ui/Switch'
import { Tag } from '../../components/ui/Tag'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { useToast } from '../../components/ui/Toast'
import { useLinks, useSetGrant, useUnlink } from '../../lib/queries'
import { errorMessage } from '../../lib/errors'
import type { LinkedRelationship } from '../../lib/types'

function LinkRow({ link }: { link: LinkedRelationship }) {
  const { t } = useTranslation()
  const toast = useToast()
  const setGrant = useSetGrant(link.partnerId)
  const unlink = useUnlink()
  const [confirm, setConfirm] = useState(false)

  const sharingCheckIns = link.grantsGivenByMe.some(
    (g) => g.scope === 'CHECK_INS' && g.status === 'ACTIVE',
  )
  const theyShare = link.grantsGivenToMe.filter((g) => g.status === 'ACTIVE').map((g) => g.scope)

  return (
    <li className="px-5 py-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div className="min-w-0">
          <p className="text-[14px] font-medium text-ink">
            {t('link.rowTitle', { owner: link.ownerDisplayName, name: link.partnerName })}
          </p>
          <p className="mt-0.5 flex flex-wrap items-center gap-1.5 text-[12.5px] text-ink-3">
            <span>@{link.ownerUsername}</span>
            {link.relationshipType && <Tag>{t(`relationshipType.${link.relationshipType}`)}</Tag>}
          </p>
        </div>
        <div className="flex items-center gap-1">
          <Link
            to={`/check-in?partner=${link.partnerId}`}
            className="inline-flex h-8 items-center gap-1.5 rounded-lg border border-border-strong bg-surface px-2.5 text-[12.5px] font-medium text-ink shadow-card transition-colors hover:bg-surface-2"
          >
            <SmilePlus size={13} aria-hidden="true" />
            {t('nav.checkin')}
          </Link>
          <button
            onClick={() => setConfirm(true)}
            aria-label={t('link.unlink')}
            title={t('link.unlink')}
            className="rounded-md p-1.5 text-ink-3 transition-colors hover:bg-surface-2 hover:text-danger"
          >
            <Unlink size={14} />
          </button>
        </div>
      </div>

      <div className="mt-3 border-t border-border">
        <Switch
          label={t('link.shareMyCheckIns')}
          description={t('link.shareMyCheckInsHint', { owner: link.ownerDisplayName })}
          checked={sharingCheckIns}
          disabled={setGrant.isPending}
          onChange={(enabled) =>
            setGrant.mutate(
              { scope: 'CHECK_INS', enabled },
              { onError: (err) => toast(errorMessage(err), 'error') },
            )
          }
        />
      </div>
      <p className="text-[12px] text-ink-3">
        {theyShare.length > 0
          ? t('link.theyShare', {
              owner: link.ownerDisplayName,
              scopes: theyShare.map((s) => t(`link.scope.${s}`)).join(' · '),
            })
          : t('link.theyShareNothing', { owner: link.ownerDisplayName })}
      </p>

      <ConfirmDialog
        open={confirm}
        title={t('link.unlinkConfirmTitle')}
        body={t('link.unlinkConfirmBody')}
        danger
        busy={unlink.isPending}
        onConfirm={() =>
          unlink.mutate(link.partnerId, {
            onSuccess: () => {
              toast(t('common.saved'))
              setConfirm(false)
            },
            onError: (err) => toast(errorMessage(err), 'error'),
          })
        }
        onClose={() => setConfirm(false)}
      />
    </li>
  )
}

/** Dashboard block: relationships where someone else invited me. */
export function LinksSection() {
  const { t } = useTranslation()
  const { data: links } = useLinks()
  if (!links || links.length === 0) return null

  return (
    <section className="card overflow-hidden">
      <header className="flex items-center gap-2 border-b border-border px-5 py-3">
        <Link2 size={14} className="text-teal" aria-hidden="true" />
        <h2 className="text-[13px] font-semibold text-ink">{t('link.dashboardTitle')}</h2>
      </header>
      <ul className="divide-y divide-border">
        {links.map((link) => (
          <LinkRow key={link.partnerId} link={link} />
        ))}
      </ul>
    </section>
  )
}
