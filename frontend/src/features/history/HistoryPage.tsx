import { useTranslation } from 'react-i18next'
import { Clock } from 'lucide-react'
import { usePartnerHistory } from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { Avatar } from '../../components/ui/Avatar'
import { Tag } from '../../components/ui/Tag'
import { formatDate, formatDuration } from '../../lib/dates'

export function HistoryPage() {
  const { t } = useTranslation()
  const { data: partners, isLoading } = usePartnerHistory()

  if (isLoading) return <PageLoader />

  return (
    <div className="mx-auto max-w-xl space-y-5">
      <header>
        <h1 className="font-display text-[21px] font-semibold text-ink">{t('history.title')}</h1>
        <p className="text-[13px] text-ink-2">{t('history.subtitle')}</p>
      </header>

      {!partners || partners.length === 0 ? (
        <div className="rounded-xl border border-border bg-surface">
          <EmptyState
            icon={<Clock size={28} strokeWidth={1.5} />}
            title={t('history.empty.title')}
            body={t('history.empty.body')}
          />
        </div>
      ) : (
        <ul className="divide-y divide-border rounded-xl border border-border bg-surface">
          {partners.map((partner) => {
            const rel = partner.relationship
            return (
              <li key={partner.id} className="flex items-center gap-3 px-4 py-3 sm:px-5">
                <Avatar emoji={partner.avatarEmoji} name={partner.name} size="sm" />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-center gap-x-2 gap-y-0.5">
                    <span className={`text-[14px] font-medium ${partner.deleted ? 'text-ink-3 line-through' : 'text-ink'}`}>
                      {partner.name}
                    </span>
                    {rel && (
                      <span className="text-[12px] text-ink-3">
                        {t(`relationshipType.${rel.type}`)}
                      </span>
                    )}
                    {partner.deleted ? (
                      <Tag tone="neutral">{t('history.deleted')}</Tag>
                    ) : rel?.status === 'ENDED' ? (
                      <Tag tone="danger">{t('history.ended')}</Tag>
                    ) : (
                      <Tag tone="teal">{t('history.active')}</Tag>
                    )}
                  </div>
                  <p className="text-[12.5px] text-ink-2">
                    {rel?.togetherSince && (
                      <>
                        {formatDate(rel.togetherSince)}
                        {rel.relationshipEndDate && <> – {formatDate(rel.relationshipEndDate)}</>}
                        {rel.duration && (
                          <> · {t('duration.together', { duration: formatDuration(rel.duration) })}</>
                        )}
                      </>
                    )}
                  </p>
                </div>
              </li>
            )
          })}
        </ul>
      )}
    </div>
  )
}
