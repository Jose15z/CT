import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Sheet } from '../../components/ui/Sheet'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { Input, Select } from '../../components/ui/Input'
import { useToast } from '../../components/ui/Toast'
import { useUpdateRelationship } from '../../lib/queries'
import { errorMessage } from '../../lib/errors'
import type { Partner, RelationshipStatus, RelationshipType } from '../../lib/types'

const TYPES: RelationshipType[] = [
  'CASUAL',
  'DATING',
  'SERIOUS_RELATIONSHIP',
  'MONOGAMOUS',
  'POLYAMOROUS',
  'ENGAGED',
  'MARRIED',
  'FRIENDS_WITH_BENEFITS',
  'OTHER',
]

const STATUSES: RelationshipStatus[] = ['ACTIVE', 'PAUSED', 'INACTIVE', 'ENDED']

interface RelationshipSheetProps {
  open: boolean
  onClose: () => void
  partner: Partner
}

export function RelationshipSheet({ open, onClose, partner }: RelationshipSheetProps) {
  const { t } = useTranslation()
  const toast = useToast()
  const updateRelationship = useUpdateRelationship(partner.id)
  const rel = partner.relationship

  const [form, setForm] = useState({
    type: 'DATING' as RelationshipType,
    status: 'ACTIVE' as RelationshipStatus,
    datingStartDate: '',
    relationshipStartDate: '',
    engagementDate: '',
    marriageDate: '',
    relationshipEndDate: '',
  })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open && rel) {
      setError(null)
      setForm({
        type: rel.type,
        status: rel.status,
        datingStartDate: rel.datingStartDate ?? '',
        relationshipStartDate: rel.relationshipStartDate ?? '',
        engagementDate: rel.engagementDate ?? '',
        marriageDate: rel.marriageDate ?? '',
        relationshipEndDate: rel.relationshipEndDate ?? '',
      })
    }
  }, [open, rel])

  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const submit = () => {
    setError(null)
    updateRelationship.mutate(
      {
        type: form.type,
        status: form.status,
        datingStartDate: form.datingStartDate || undefined,
        relationshipStartDate: form.relationshipStartDate || undefined,
        engagementDate: form.engagementDate || undefined,
        marriageDate: form.marriageDate || undefined,
        relationshipEndDate:
          form.status === 'ENDED' ? form.relationshipEndDate || undefined : undefined,
      },
      {
        onSuccess: () => {
          toast(t('common.saved'))
          onClose()
        },
        onError: (err) => setError(errorMessage(err)),
      },
    )
  }

  return (
    <Sheet open={open} onClose={onClose} title={t('partner.detail.relationship')}>
      <div className="space-y-4">
        <div className="grid grid-cols-2 gap-3">
          <Field label={t('partner.relationshipType')} htmlFor="rel-type">
            <Select
              id="rel-type"
              value={form.type}
              onChange={(e) => set('type', e.target.value as RelationshipType)}
            >
              {TYPES.map((type) => (
                <option key={type} value={type}>
                  {t(`relationshipType.${type}`)}
                </option>
              ))}
            </Select>
          </Field>
          <Field label={t('partner.status')} htmlFor="rel-status">
            <Select
              id="rel-status"
              value={form.status}
              onChange={(e) => set('status', e.target.value as RelationshipStatus)}
            >
              {STATUSES.map((status) => (
                <option key={status} value={status}>
                  {t(`relationshipStatus.${status}`)}
                </option>
              ))}
            </Select>
          </Field>
        </div>

        <div className="grid grid-cols-2 gap-3">
          <Field label={t('partner.dates.datingStart')} htmlFor="rel-dating">
            <Input
              id="rel-dating"
              type="date"
              value={form.datingStartDate}
              onChange={(e) => set('datingStartDate', e.target.value)}
            />
          </Field>
          <Field label={t('partner.dates.relationshipStart')} htmlFor="rel-start">
            <Input
              id="rel-start"
              type="date"
              value={form.relationshipStartDate}
              onChange={(e) => set('relationshipStartDate', e.target.value)}
            />
          </Field>
          <Field label={t('partner.dates.engagement')} htmlFor="rel-engagement">
            <Input
              id="rel-engagement"
              type="date"
              value={form.engagementDate}
              onChange={(e) => set('engagementDate', e.target.value)}
            />
          </Field>
          <Field label={t('partner.dates.marriage')} htmlFor="rel-marriage">
            <Input
              id="rel-marriage"
              type="date"
              value={form.marriageDate}
              onChange={(e) => set('marriageDate', e.target.value)}
            />
          </Field>
          {form.status === 'ENDED' && (
            <Field label={t('partner.dates.end')} htmlFor="rel-end">
              <Input
                id="rel-end"
                type="date"
                value={form.relationshipEndDate}
                onChange={(e) => set('relationshipEndDate', e.target.value)}
              />
            </Field>
          )}
        </div>

        {error && <p className="text-[13px] text-danger">{error}</p>}

        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={updateRelationship.isPending}>
            {t('common.cancel')}
          </Button>
          <Button onClick={submit} disabled={updateRelationship.isPending}>
            {updateRelationship.isPending ? t('common.saving') : t('common.save')}
          </Button>
        </div>
      </div>
    </Sheet>
  )
}
