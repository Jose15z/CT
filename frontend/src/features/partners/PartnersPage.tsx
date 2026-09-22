import { useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ChevronRight, Heart, Plus, Link2 } from 'lucide-react'
import { usePartners } from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { Avatar } from '../../components/ui/Avatar'
import { Tag } from '../../components/ui/Tag'
import { Button } from '../../components/ui/Button'
import { PartnerFormSheet } from './PartnerFormSheet'
import { formatDate, formatDuration } from '../../lib/dates'

export function PartnersPage() {
  const { t } = useTranslation()
  const { data: partners, isLoading } = usePartners()
  const [formOpen, setFormOpen] = useState(false)

  if (isLoading) return <PageLoader />

  return (
    <div className="space-y-5">
      <header className="flex items-center justify-between">
        <h1 className="font-display text-[21px] font-semibold text-ink">
          {t('partner.listTitle')}
        </h1>
        <Button size="sm" icon={<Plus size={14} />} onClick={() => setFormOpen(true)}>
          {t('partner.add')}
        </Button>
      </header>

      {!partners || partners.length === 0 ? (
        <div className="rounded-xl border border-border bg-surface">
          <EmptyState
            icon={<Heart size={28} strokeWidth={1.5} />}
            title={t('partner.empty.title')}
            body={t('partner.empty.body')}
            action={
              <Button icon={<Plus size={15} />} onClick={() => setFormOpen(true)}>
                {t('partner.empty.action')}
              </Button>
            }
          />
        </div>
      ) : (
        <ul className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
          {partners.map((partner) => {
            const rel = partner.relationship
            return (
              <li key={partner.id}>
                <Link
                  to={`/partners/${partner.id}`}
                  className="flex items-center gap-3 px-4 py-3.5 transition-colors hover:bg-surface-2 sm:px-5"
                >
                  <Avatar emoji={partner.avatarEmoji} name={partner.name} />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
                      <span className="text-[14.5px] font-medium text-ink">{partner.name}</span>
                      {rel && (
                        <span className="text-[12px] text-ink-3">
                          {t(`relationshipType.${rel.type}`)}
                        </span>
                      )}
                      {partner.linked && (
                        <Tag tone="teal">
                          <Link2 size={11} aria-hidden="true" />
                          {t('partner.linked')}
                        </Tag>
                      )}
                      {rel && rel.status !== 'ACTIVE' && (
                        <Tag tone={rel.status === 'ENDED' ? 'danger' : 'neutral'}>
                          {t(`relationshipStatus.${rel.status}`)}
                        </Tag>
                      )}
                    </div>
                    {rel?.togetherSince && rel.duration && (
                      <p className="text-[12.5px] text-ink-2">
                        {t('partner.since', { date: formatDate(rel.togetherSince) })}
                        {' · '}
                        {t('duration.together', { duration: formatDuration(rel.duration) })}
                      </p>
                    )}
                  </div>
                  <ChevronRight size={16} className="shrink-0 text-ink-3" aria-hidden="true" />
                </Link>
              </li>
            )
          })}
        </ul>
      )}

      <PartnerFormSheet open={formOpen} onClose={() => setFormOpen(false)} />
    </div>
  )
}
