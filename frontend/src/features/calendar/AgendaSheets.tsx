import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Sheet } from '../../components/ui/Sheet'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { Input, Select, Textarea } from '../../components/ui/Input'
import { useToast } from '../../components/ui/Toast'
import { useCreateDatePlan, useLogEncounter } from '../../lib/queries'
import { errorMessage } from '../../lib/errors'
import { todayISO } from '../../lib/dates'
import type { Partner } from '../../lib/types'

interface AgendaSheetProps {
  open: boolean
  onClose: () => void
  partners: Partner[]
  /** Preselected day (the one selected in the calendar grid). */
  initialDate: string
}

export function SchedulePlanSheet({ open, onClose, partners, initialDate }: AgendaSheetProps) {
  const { t } = useTranslation()
  const toast = useToast()
  const createPlan = useCreateDatePlan()

  const [form, setForm] = useState({
    partnerId: '',
    title: '',
    date: initialDate,
    startTime: '',
    location: '',
    notes: '',
  })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setError(null)
      setForm({
        partnerId: partners[0]?.id ?? '',
        title: '',
        date: initialDate,
        startTime: '',
        location: '',
        notes: '',
      })
    }
  }, [open, partners, initialDate])

  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const submit = () => {
    setError(null)
    createPlan.mutate(
      {
        partnerId: form.partnerId,
        title: form.title.trim(),
        date: form.date,
        startTime: form.startTime || undefined,
        location: form.location.trim() || undefined,
        notes: form.notes.trim() || undefined,
      },
      {
        onSuccess: () => {
          toast(t('agenda.planSaved'))
          onClose()
        },
        onError: (err) => setError(errorMessage(err)),
      },
    )
  }

  return (
    <Sheet open={open} onClose={onClose} title={t('agenda.form.planTitle')}>
      <div className="space-y-4">
        <Field label={t('agenda.form.partner')} htmlFor="plan-partner">
          <Select
            id="plan-partner"
            value={form.partnerId}
            onChange={(e) => set('partnerId', e.target.value)}
          >
            {partners.map((partner) => (
              <option key={partner.id} value={partner.id}>
                {partner.nickname ?? partner.name}
              </option>
            ))}
          </Select>
        </Field>

        <Field label={t('agenda.form.title')} htmlFor="plan-title">
          <Input
            id="plan-title"
            value={form.title}
            onChange={(e) => set('title', e.target.value)}
            placeholder={t('agenda.form.titlePlaceholder')}
            maxLength={120}
            required
          />
        </Field>

        <div className="grid grid-cols-2 gap-3">
          <Field label={t('agenda.form.date')} htmlFor="plan-date">
            <Input
              id="plan-date"
              type="date"
              value={form.date}
              onChange={(e) => set('date', e.target.value)}
              required
            />
          </Field>
          <Field label={t('agenda.form.time')} htmlFor="plan-time">
            <Input
              id="plan-time"
              type="time"
              value={form.startTime}
              onChange={(e) => set('startTime', e.target.value)}
            />
          </Field>
        </div>

        <Field label={`${t('agenda.form.location')} (${t('common.optional')})`} htmlFor="plan-location">
          <Input
            id="plan-location"
            value={form.location}
            onChange={(e) => set('location', e.target.value)}
            maxLength={120}
          />
        </Field>

        <Field label={`${t('agenda.form.notes')} (${t('common.optional')})`} htmlFor="plan-notes">
          <Textarea
            id="plan-notes"
            value={form.notes}
            onChange={(e) => set('notes', e.target.value)}
            rows={2}
          />
        </Field>

        {error && <p className="text-[13px] text-danger">{error}</p>}

        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={createPlan.isPending}>
            {t('common.cancel')}
          </Button>
          <Button
            onClick={submit}
            disabled={createPlan.isPending || !form.title.trim() || !form.partnerId || !form.date}
          >
            {createPlan.isPending ? t('common.saving') : t('common.save')}
          </Button>
        </div>
      </div>
    </Sheet>
  )
}

export function LogEncounterSheet({ open, onClose, partners, initialDate }: AgendaSheetProps) {
  const { t } = useTranslation()
  const toast = useToast()
  const logEncounter = useLogEncounter()
  const today = todayISO()

  const [form, setForm] = useState({ partnerId: '', date: initialDate, notes: '' })
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setError(null)
      // The log never accepts future days; clamp a future selection to today.
      setForm({
        partnerId: partners[0]?.id ?? '',
        date: initialDate > today ? today : initialDate,
        notes: '',
      })
    }
  }, [open, partners, initialDate, today])

  const set = <K extends keyof typeof form>(key: K, value: (typeof form)[K]) =>
    setForm((prev) => ({ ...prev, [key]: value }))

  const submit = () => {
    setError(null)
    logEncounter.mutate(
      {
        partnerId: form.partnerId,
        date: form.date,
        notes: form.notes.trim() || undefined,
      },
      {
        onSuccess: () => {
          toast(t('agenda.encounterSaved'))
          onClose()
        },
        onError: (err) => setError(errorMessage(err)),
      },
    )
  }

  return (
    <Sheet open={open} onClose={onClose} title={t('agenda.form.encounterTitle')}>
      <div className="space-y-4">
        <Field label={t('agenda.form.partner')} htmlFor="encounter-partner">
          <Select
            id="encounter-partner"
            value={form.partnerId}
            onChange={(e) => set('partnerId', e.target.value)}
          >
            {partners.map((partner) => (
              <option key={partner.id} value={partner.id}>
                {partner.nickname ?? partner.name}
              </option>
            ))}
          </Select>
        </Field>

        <Field label={t('agenda.form.date')} htmlFor="encounter-date">
          <Input
            id="encounter-date"
            type="date"
            max={today}
            value={form.date}
            onChange={(e) => set('date', e.target.value)}
            required
          />
        </Field>

        <Field label={`${t('agenda.form.notes')} (${t('common.optional')})`} htmlFor="encounter-notes">
          <Textarea
            id="encounter-notes"
            value={form.notes}
            onChange={(e) => set('notes', e.target.value)}
            rows={2}
          />
        </Field>

        {error && <p className="text-[13px] text-danger">{error}</p>}

        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={logEncounter.isPending}>
            {t('common.cancel')}
          </Button>
          <Button
            onClick={submit}
            disabled={logEncounter.isPending || !form.partnerId || !form.date || form.date > today}
          >
            {logEncounter.isPending ? t('common.saving') : t('common.save')}
          </Button>
        </div>
      </div>
    </Sheet>
  )
}
