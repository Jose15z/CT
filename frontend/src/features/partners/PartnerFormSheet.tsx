import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Sheet } from '../../components/ui/Sheet'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { Input, Select, Textarea } from '../../components/ui/Input'
import { useToast } from '../../components/ui/Toast'
import { useCreatePartner, useUpdatePartner } from '../../lib/queries'
import { errorMessage } from '../../lib/errors'
import { todayISO } from '../../lib/dates'
import type { Partner, RelationshipType } from '../../lib/types'

const RELATIONSHIP_TYPES: RelationshipType[] = [
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

const AVATAR_PRESETS = ['🌸', '🌙', '☀️', '🌊', '🔥', '🍒', '🦋', '⭐']

/** Latest selectable birth date: exactly 18 years ago (adults only). */
function maxAdultBirthDate(): string {
  const d = new Date()
  d.setFullYear(d.getFullYear() - 18)
  return d.toISOString().slice(0, 10)
}

interface PartnerFormSheetProps {
  open: boolean
  onClose: () => void
  /** When set, the sheet edits instead of creating. */
  partner?: Partner
}

export function PartnerFormSheet({ open, onClose, partner }: PartnerFormSheetProps) {
  const { t } = useTranslation()
  const toast = useToast()
  const createPartner = useCreatePartner()
  const updatePartner = useUpdatePartner(partner?.id ?? '')
  const editing = !!partner

  const [form, setForm] = useState({
    name: '',
    nickname: '',
    avatarEmoji: '',
    notes: '',
    birthDate: '',
    weightKg: '',
    relationshipType: 'DATING' as RelationshipType,
    datingStartDate: '',
    relationshipStartDate: '',
    engagementDate: '',
    marriageDate: '',
    consentConfirmed: false,
  })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setError(null)
      setForm({
        name: partner?.name ?? '',
        nickname: partner?.nickname ?? '',
        avatarEmoji: partner?.avatarEmoji ?? '',
        notes: partner?.notes ?? '',
        birthDate: partner?.birthDate ?? '',
        weightKg: partner?.weightKg != null ? String(partner.weightKg) : '',
        relationshipType: partner?.relationship?.type ?? 'DATING',
        datingStartDate: partner?.relationship?.datingStartDate ?? '',
        relationshipStartDate: partner?.relationship?.relationshipStartDate ?? '',
        engagementDate: partner?.relationship?.engagementDate ?? '',
        marriageDate: partner?.relationship?.marriageDate ?? '',
        consentConfirmed: false,
      })
    }
  }, [open, partner])

  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const showEngagement = form.relationshipType === 'ENGAGED' || form.relationshipType === 'MARRIED'
  const showMarriage = form.relationshipType === 'MARRIED'
  const busy = createPartner.isPending || updatePartner.isPending

  const submit = () => {
    setError(null)
    const xpFields = {
      birthDate: form.birthDate || undefined,
      weightKg: form.weightKg ? Number(form.weightKg) : undefined,
    }
    if (editing) {
      updatePartner.mutate(
        {
          name: form.name.trim(),
          nickname: form.nickname,
          notes: form.notes,
          avatarEmoji: form.avatarEmoji,
          ...xpFields,
        },
        {
          onSuccess: () => {
            toast(t('common.saved'))
            onClose()
          },
          onError: (err) => setError(errorMessage(err)),
        },
      )
    } else {
      createPartner.mutate(
        {
          name: form.name.trim(),
          nickname: form.nickname || undefined,
          notes: form.notes || undefined,
          avatarEmoji: form.avatarEmoji || undefined,
          relationshipType: form.relationshipType,
          datingStartDate: form.datingStartDate || undefined,
          relationshipStartDate: form.relationshipStartDate || undefined,
          engagementDate: showEngagement ? form.engagementDate || undefined : undefined,
          marriageDate: showMarriage ? form.marriageDate || undefined : undefined,
          consentConfirmed: form.consentConfirmed,
          ...xpFields,
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
  }

  return (
    <Sheet
      open={open}
      onClose={onClose}
      title={editing ? t('partner.form.editTitle') : t('partner.form.createTitle')}
    >
      <div className="space-y-4">
        <Field label={t('partner.name')} htmlFor="partner-name">
          <Input
            id="partner-name"
            value={form.name}
            onChange={(e) => set('name', e.target.value)}
            maxLength={60}
            required
          />
        </Field>

        <div className="grid grid-cols-2 gap-3">
          <Field
            label={`${t('partner.nickname')} (${t('common.optional')})`}
            htmlFor="partner-nickname"
          >
            <Input
              id="partner-nickname"
              value={form.nickname}
              onChange={(e) => set('nickname', e.target.value)}
              maxLength={60}
            />
          </Field>
          <Field label={`${t('partner.avatar')} (${t('common.optional')})`} htmlFor="partner-avatar">
            <Input
              id="partner-avatar"
              value={form.avatarEmoji}
              onChange={(e) => set('avatarEmoji', e.target.value)}
              maxLength={4}
            />
          </Field>
        </div>
        <div className="flex flex-wrap gap-1">
          {AVATAR_PRESETS.map((emoji) => (
            <button
              key={emoji}
              type="button"
              onClick={() => set('avatarEmoji', emoji)}
              aria-pressed={form.avatarEmoji === emoji}
              className={`rounded-md px-1.5 py-1 text-[17px] hover:bg-surface-2 ${
                form.avatarEmoji === emoji ? 'bg-peach-soft' : ''
              }`}
            >
              {emoji}
            </button>
          ))}
        </div>

        {!editing && (
          <>
            <Field label={t('partner.relationshipType')} htmlFor="partner-type">
              <Select
                id="partner-type"
                value={form.relationshipType}
                onChange={(e) => set('relationshipType', e.target.value as RelationshipType)}
              >
                {RELATIONSHIP_TYPES.map((type) => (
                  <option key={type} value={type}>
                    {t(`relationshipType.${type}`)}
                  </option>
                ))}
              </Select>
            </Field>

            <div className="grid grid-cols-2 gap-3">
              <Field
                label={`${t('partner.dates.datingStart')} (${t('common.optional')})`}
                htmlFor="partner-dating"
              >
                <Input
                  id="partner-dating"
                  type="date"
                  max={todayISO()}
                  value={form.datingStartDate}
                  onChange={(e) => set('datingStartDate', e.target.value)}
                />
              </Field>
              <Field
                label={`${t('partner.dates.relationshipStart')} (${t('common.optional')})`}
                htmlFor="partner-start"
              >
                <Input
                  id="partner-start"
                  type="date"
                  max={todayISO()}
                  value={form.relationshipStartDate}
                  onChange={(e) => set('relationshipStartDate', e.target.value)}
                />
              </Field>
            </div>

            {showEngagement && (
              <div className="grid grid-cols-2 gap-3">
                <Field label={t('partner.dates.engagement')} htmlFor="partner-engagement">
                  <Input
                    id="partner-engagement"
                    type="date"
                    value={form.engagementDate}
                    onChange={(e) => set('engagementDate', e.target.value)}
                  />
                </Field>
                {showMarriage && (
                  <Field label={t('partner.dates.marriage')} htmlFor="partner-marriage">
                    <Input
                      id="partner-marriage"
                      type="date"
                      value={form.marriageDate}
                      onChange={(e) => set('marriageDate', e.target.value)}
                    />
                  </Field>
                )}
              </div>
            )}
          </>
        )}

        <fieldset className="rounded-md border border-border p-3">
          <legend className="px-1 text-[12px] font-medium text-ink-2">
            {t('partner.xpFields')}
          </legend>
          <div className="grid grid-cols-2 gap-3">
            <Field label={t('partner.birthDate')} htmlFor="partner-birth">
              <Input
                id="partner-birth"
                type="date"
                max={maxAdultBirthDate()}
                value={form.birthDate}
                onChange={(e) => set('birthDate', e.target.value)}
              />
            </Field>
            <Field label={t('partner.weight')} htmlFor="partner-weight">
              <Input
                id="partner-weight"
                type="number"
                min={30}
                max={300}
                step={0.5}
                value={form.weightKg}
                onChange={(e) => set('weightKg', e.target.value)}
              />
            </Field>
          </div>
          <p className="mt-1.5 text-[11.5px] leading-snug text-ink-3">
            {t('partner.xpFieldsHint')}
          </p>
        </fieldset>

        <Field label={`${t('partner.notes')} (${t('common.optional')})`} htmlFor="partner-notes">
          <Textarea
            id="partner-notes"
            value={form.notes}
            onChange={(e) => set('notes', e.target.value)}
            rows={2}
          />
        </Field>

        {!editing && (
          <label className="flex cursor-pointer items-start gap-2.5 rounded-md border border-border bg-surface-2 p-3">
            <input
              type="checkbox"
              checked={form.consentConfirmed}
              onChange={(e) => set('consentConfirmed', e.target.checked)}
              className="mt-0.5 h-4 w-4 accent-[var(--peach)]"
            />
            <span className="text-[12.5px] leading-relaxed text-ink-2">
              {t('partner.consentLabel')}
            </span>
          </label>
        )}

        {error && <p className="text-[13px] text-danger">{error}</p>}

        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={busy}>
            {t('common.cancel')}
          </Button>
          <Button
            onClick={submit}
            disabled={busy || !form.name.trim() || (!editing && !form.consentConfirmed)}
          >
            {busy ? t('common.saving') : t('common.save')}
          </Button>
        </div>
      </div>
    </Sheet>
  )
}
