import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  ArrowLeft,
  CalendarDays,
  Clock,
  Link2,
  Pencil,
  Trash2,
} from 'lucide-react'
import { usePartner, useDeletePartner, useUpdateRelationship, useObservations } from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { Avatar } from '../../components/ui/Avatar'
import { Tag } from '../../components/ui/Tag'
import { Button } from '../../components/ui/Button'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { useToast } from '../../components/ui/Toast'
import { PartnerFormSheet } from './PartnerFormSheet'
import { RelationshipSheet } from './RelationshipSheet'
import { CycleSection } from './CycleSection'
import { observationEmoji } from '../../lib/emoji'
import { errorMessage } from '../../lib/errors'
import { formatDate, formatDayMonth, formatDuration } from '../../lib/dates'

export function PartnerDetailPage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  const navigate = useNavigate()
  const toast = useToast()
  const { data: partner, isLoading } = usePartner(id)
  const { data: observations } = useObservations(id)
  const deletePartner = useDeletePartner()
  const updateRelationship = useUpdateRelationship(id ?? '')

  const [editOpen, setEditOpen] = useState(false)
  const [relationshipOpen, setRelationshipOpen] = useState(false)
  const [confirm, setConfirm] = useState<'delete' | 'end' | null>(null)

  if (isLoading) return <PageLoader />
  if (!partner) return null
  const rel = partner.relationship

  const dateRows = rel
    ? ([
        ['datingStart', rel.datingStartDate],
        ['relationshipStart', rel.relationshipStartDate],
        ['engagement', rel.engagementDate],
        ['marriage', rel.marriageDate],
        ['end', rel.relationshipEndDate],
      ] as const).filter(([, value]) => value)
    : []

  const endRelationship = () => {
    updateRelationship.mutate(
      { status: 'ENDED' },
      {
        onSuccess: () => {
          toast(t('common.saved'))
          setConfirm(null)
        },
        onError: (err) => {
          toast(errorMessage(err), 'error')
          setConfirm(null)
        },
      },
    )
  }

  const reactivate = () => {
    updateRelationship.mutate(
      { status: 'ACTIVE' },
      {
        onSuccess: () => toast(t('common.saved')),
        onError: (err) => toast(errorMessage(err), 'error'),
      },
    )
  }

  return (
    <div className="space-y-6">
      <Link
        to="/partners"
        className="inline-flex items-center gap-1.5 text-[13px] font-medium text-ink-3 hover:text-ink"
      >
        <ArrowLeft size={14} aria-hidden="true" />
        {t('partner.listTitle')}
      </Link>

      {/* Header */}
      <header className="flex items-start gap-4">
        <Avatar emoji={partner.avatarEmoji} name={partner.name} size="lg" />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <h1 className="font-display text-[21px] font-semibold text-ink">{partner.name}</h1>
            {partner.nickname && <span className="text-[14px] text-ink-3">«{partner.nickname}»</span>}
          </div>
          <div className="mt-1 flex flex-wrap items-center gap-1.5">
            {rel && <Tag tone="peach">{t(`relationshipType.${rel.type}`)}</Tag>}
            {rel && rel.status !== 'ACTIVE' && (
              <Tag tone={rel.status === 'ENDED' ? 'danger' : 'neutral'}>
                {t(`relationshipStatus.${rel.status}`)}
              </Tag>
            )}
            {partner.linked && (
              <Tag tone="teal">
                <Link2 size={11} aria-hidden="true" />
                {t('partner.linked')}
              </Tag>
            )}
          </div>
          {rel?.togetherSince && rel.duration && (
            <p className="mt-1.5 text-[13.5px] text-ink-2">
              {t('partner.since', { date: formatDate(rel.togetherSince) })}
              {' · '}
              {t('duration.together', { duration: formatDuration(rel.duration) })}
            </p>
          )}
        </div>
        <Button
          variant="secondary"
          size="sm"
          icon={<Pencil size={13} />}
          onClick={() => setEditOpen(true)}
        >
          {t('common.edit')}
        </Button>
      </header>

      {/* Quick links */}
      <div className="flex flex-wrap gap-2">
        <Link
          to={`/partners/${partner.id}/calendar`}
          className="inline-flex h-8 items-center gap-1.5 rounded-md border border-border-strong px-2.5 text-[13px] font-medium text-ink hover:bg-surface-2"
        >
          <CalendarDays size={14} aria-hidden="true" />
          {t('nav.calendar')}
        </Link>
        <Link
          to={`/partners/${partner.id}/timeline`}
          className="inline-flex h-8 items-center gap-1.5 rounded-md border border-border-strong px-2.5 text-[13px] font-medium text-ink hover:bg-surface-2"
        >
          <Clock size={14} aria-hidden="true" />
          {t('partner.detail.timeline')}
        </Link>
      </div>

      <div className="space-y-6 rounded-xl border border-border bg-surface p-4 sm:p-5">
        {/* Relationship dates */}
        <section>
          <div className="flex items-center justify-between">
            <h2 className="text-[15px] font-semibold text-ink">
              {t('partner.detail.relationship')}
            </h2>
            <Button
              variant="ghost"
              size="sm"
              icon={<Pencil size={13} />}
              onClick={() => setRelationshipOpen(true)}
            >
              {t('common.edit')}
            </Button>
          </div>
          {dateRows.length > 0 ? (
            <dl className="mt-2 space-y-1.5">
              {dateRows.map(([key, value]) => (
                <div key={key} className="flex justify-between gap-4 text-[13.5px]">
                  <dt className="text-ink-2">{t(`partner.dates.${key}`)}</dt>
                  <dd className="font-medium text-ink">{formatDate(value!)}</dd>
                </div>
              ))}
              {rel?.nextAnniversary && (
                <div className="flex justify-between gap-4 border-t border-border pt-1.5 text-[13.5px]">
                  <dt className="text-ink-2">{t('dashboard.nextAnniversary')}</dt>
                  <dd className="font-medium text-ink">
                    {formatDayMonth(rel.nextAnniversary.date)}
                    {' · '}
                    {rel.nextAnniversary.daysUntil === 0
                      ? t('common.today')
                      : t('common.inDays', { count: rel.nextAnniversary.daysUntil })}
                  </dd>
                </div>
              )}
            </dl>
          ) : (
            <p className="mt-2 text-[13.5px] text-ink-3">—</p>
          )}
          {partner.notes && (
            <p className="mt-3 border-t border-border pt-3 text-[13.5px] text-ink-2">
              {partner.notes}
            </p>
          )}
        </section>

        <div className="border-t border-border" />

        {/* Cycle */}
        <CycleSection partnerId={partner.id} partnerName={partner.nickname ?? partner.name} />

        {/* Observations */}
        {observations && observations.length > 0 && (
          <>
            <div className="border-t border-border" />
            <section>
              <h2 className="text-[15px] font-semibold text-ink">
                {t('partner.detail.observations')}
              </h2>
              <p className="mt-0.5 text-[12px] text-ink-3">{t('checkin.observationDisclaimer')}</p>
              <ul className="mt-2 space-y-1.5">
                {observations.slice(0, 7).map((observation) => (
                  <li key={observation.id} className="flex items-center gap-2.5 text-[13.5px]">
                    <span aria-hidden="true">{observationEmoji[observation.observationType]}</span>
                    <span className="text-ink">
                      {t(`observationType.${observation.observationType}`)}
                    </span>
                    {observation.note && (
                      <span className="min-w-0 flex-1 truncate text-ink-3">{observation.note}</span>
                    )}
                    <span className="ml-auto shrink-0 text-[12px] text-ink-3">
                      {formatDate(observation.date)}
                    </span>
                  </li>
                ))}
              </ul>
            </section>
          </>
        )}
      </div>

      {/* Danger zone */}
      <section className="flex flex-wrap gap-2">
        {rel?.status === 'ACTIVE' ? (
          <Button variant="secondary" size="sm" onClick={() => setConfirm('end')}>
            {t('partner.detail.endRelationship')}
          </Button>
        ) : rel?.status === 'ENDED' ? (
          <Button variant="secondary" size="sm" onClick={reactivate}>
            {t('partner.detail.reactivate')}
          </Button>
        ) : null}
        <Button
          variant="danger"
          size="sm"
          icon={<Trash2 size={13} />}
          onClick={() => setConfirm('delete')}
        >
          {t('partner.detail.deleteRecord')}
        </Button>
      </section>

      <PartnerFormSheet open={editOpen} onClose={() => setEditOpen(false)} partner={partner} />
      <RelationshipSheet
        open={relationshipOpen}
        onClose={() => setRelationshipOpen(false)}
        partner={partner}
      />
      <ConfirmDialog
        open={confirm === 'end'}
        title={t('partner.detail.endConfirmTitle', { name: partner.name })}
        body={t('partner.detail.endConfirmBody')}
        busy={updateRelationship.isPending}
        onConfirm={endRelationship}
        onClose={() => setConfirm(null)}
      />
      <ConfirmDialog
        open={confirm === 'delete'}
        title={t('partner.detail.deleteConfirmTitle', { name: partner.name })}
        body={t('partner.detail.deleteConfirmBody')}
        danger
        busy={deletePartner.isPending}
        onConfirm={() =>
          deletePartner.mutate(partner.id, {
            onSuccess: () => {
              toast(t('common.saved'))
              navigate('/partners')
            },
          })
        }
        onClose={() => setConfirm(null)}
      />
    </div>
  )
}
