import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ArrowLeft, Heart, Plus, Trash2 } from 'lucide-react'
import { useCreateMilestone, useDeleteMilestone, useMilestones, usePartner } from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { Button } from '../../components/ui/Button'
import { Sheet } from '../../components/ui/Sheet'
import { Field } from '../../components/ui/Field'
import { Input, Select, Textarea } from '../../components/ui/Input'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { useToast } from '../../components/ui/Toast'
import { errorMessage } from '../../lib/errors'
import { formatDate, formatDuration } from '../../lib/dates'
import type { MilestoneType } from '../../lib/types'

const MILESTONE_TYPES: MilestoneType[] = [
  'FIRST_DATE',
  'STARTED_DATING',
  'RELATIONSHIP_STARTED',
  'ENGAGEMENT',
  'MARRIAGE',
  'MOVED_IN_TOGETHER',
  'TRIP',
  'CUSTOM',
]

export function TimelinePage() {
  const { id } = useParams<{ id: string }>()
  const { t } = useTranslation()
  const toast = useToast()
  const { data: partner, isLoading } = usePartner(id)
  const { data: milestones } = useMilestones(id)
  const createMilestone = useCreateMilestone(id ?? '')
  const deleteMilestone = useDeleteMilestone(id ?? '')

  const [formOpen, setFormOpen] = useState(false)
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [form, setForm] = useState({
    type: 'CUSTOM' as MilestoneType,
    title: '',
    description: '',
    date: '',
  })
  const [error, setError] = useState<string | null>(null)

  if (isLoading) return <PageLoader />
  if (!partner) return null

  const openForm = () => {
    setForm({ type: 'CUSTOM', title: '', description: '', date: '' })
    setError(null)
    setFormOpen(true)
  }

  const pickType = (type: MilestoneType) => {
    setForm((prev) => ({
      ...prev,
      type,
      // Prefill the title with the type name; CUSTOM keeps whatever was typed.
      title: type === 'CUSTOM' ? prev.title : t(`milestoneType.${type}`),
    }))
  }

  const submit = () => {
    setError(null)
    createMilestone.mutate(
      {
        type: form.type,
        title: form.title.trim(),
        description: form.description.trim() || undefined,
        date: form.date,
      },
      {
        onSuccess: () => {
          toast(t('common.saved'))
          setFormOpen(false)
        },
        onError: (err) => setError(errorMessage(err)),
      },
    )
  }

  return (
    <div className="space-y-5">
      <Link
        to={`/partners/${partner.id}`}
        className="inline-flex items-center gap-1.5 text-[13px] font-medium text-ink-3 hover:text-ink"
      >
        <ArrowLeft size={14} aria-hidden="true" />
        {partner.name}
      </Link>

      <header className="flex items-center justify-between">
        <div>
          <h1 className="font-display text-[21px] font-semibold text-ink">{t('milestone.title')}</h1>
          {partner.relationship?.duration && (
            <p className="text-[13px] text-ink-2">
              {t('duration.together', {
                duration: formatDuration(partner.relationship.duration),
              })}
            </p>
          )}
        </div>
        <Button size="sm" icon={<Plus size={14} />} onClick={openForm}>
          {t('milestone.add')}
        </Button>
      </header>

      {!milestones || milestones.length === 0 ? (
        <div className="rounded-xl border border-border bg-surface">
          <EmptyState
            icon={<Heart size={28} strokeWidth={1.5} />}
            title={t('milestone.empty.title')}
            body={t('milestone.empty.body')}
            action={
              <Button icon={<Plus size={15} />} onClick={openForm}>
                {t('milestone.add')}
              </Button>
            }
          />
        </div>
      ) : (
        <ol className="relative ml-2 space-y-5 border-l border-border-strong pl-5">
          {milestones.map((milestone) => (
            <li key={milestone.id} className="relative">
              <span
                aria-hidden="true"
                className="absolute -left-[26.5px] top-1.5 h-2.5 w-2.5 rounded-full border-2 border-plum bg-surface"
              />
              <div className="group">
                <p className="text-[12px] font-medium uppercase tracking-wide text-ink-3">
                  {formatDate(milestone.date)}
                </p>
                <div className="flex items-start gap-2">
                  <h3 className="text-[14.5px] font-semibold text-ink">{milestone.title}</h3>
                  <button
                    onClick={() => setDeleteId(milestone.id)}
                    aria-label={t('common.delete')}
                    className="mt-0.5 rounded-md p-1 text-ink-3 opacity-0 transition-opacity hover:bg-danger-soft hover:text-danger focus-visible:opacity-100 group-hover:opacity-100"
                  >
                    <Trash2 size={13} />
                  </button>
                </div>
                {milestone.description && (
                  <p className="mt-0.5 text-[13.5px] text-ink-2">{milestone.description}</p>
                )}
              </div>
            </li>
          ))}
        </ol>
      )}

      <Sheet open={formOpen} onClose={() => setFormOpen(false)} title={t('milestone.formTitle')}>
        <div className="space-y-4">
          <Field label={t('milestone.type')} htmlFor="milestone-type">
            <Select
              id="milestone-type"
              value={form.type}
              onChange={(e) => pickType(e.target.value as MilestoneType)}
            >
              {MILESTONE_TYPES.map((type) => (
                <option key={type} value={type}>
                  {t(`milestoneType.${type}`)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('milestone.milestoneTitle')} htmlFor="milestone-title">
            <Input
              id="milestone-title"
              value={form.title}
              onChange={(e) => setForm((prev) => ({ ...prev, title: e.target.value }))}
              maxLength={120}
            />
          </Field>
          <Field label={t('milestone.date')} htmlFor="milestone-date">
            <Input
              id="milestone-date"
              type="date"
              value={form.date}
              onChange={(e) => setForm((prev) => ({ ...prev, date: e.target.value }))}
            />
          </Field>
          <Field
            label={`${t('milestone.description')} (${t('common.optional')})`}
            htmlFor="milestone-description"
            error={error ?? undefined}
          >
            <Textarea
              id="milestone-description"
              value={form.description}
              onChange={(e) => setForm((prev) => ({ ...prev, description: e.target.value }))}
              rows={2}
            />
          </Field>
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setFormOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button
              onClick={submit}
              disabled={createMilestone.isPending || !form.title.trim() || !form.date}
            >
              {createMilestone.isPending ? t('common.saving') : t('common.save')}
            </Button>
          </div>
        </div>
      </Sheet>

      <ConfirmDialog
        open={deleteId !== null}
        title={t('milestone.deleteConfirm')}
        body={t('common.irreversible')}
        danger
        busy={deleteMilestone.isPending}
        onConfirm={() => {
          if (deleteId) {
            deleteMilestone.mutate(deleteId, { onSuccess: () => setDeleteId(null) })
          }
        }}
        onClose={() => setDeleteId(null)}
      />
    </div>
  )
}
