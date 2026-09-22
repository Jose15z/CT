import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Sheet } from '../../components/ui/Sheet'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { Input, Textarea } from '../../components/ui/Input'
import { SegmentedControl } from '../../components/ui/SegmentedControl'
import { useToast } from '../../components/ui/Toast'
import { useLogPeriod } from '../../lib/queries'
import { errorMessage } from '../../lib/errors'
import { todayISO } from '../../lib/dates'

interface LogPeriodSheetProps {
  open: boolean
  onClose: () => void
  partnerId: string
  partnerName: string
  initialDate?: string
}

/** Quick flow: partner is fixed, "today" is one tap, past dates one more. */
export function LogPeriodSheet({
  open,
  onClose,
  partnerId,
  partnerName,
  initialDate,
}: LogPeriodSheetProps) {
  const { t } = useTranslation()
  const toast = useToast()
  const logPeriod = useLogPeriod(partnerId)
  const [mode, setMode] = useState<'today' | 'other'>(initialDate ? 'other' : 'today')
  const [date, setDate] = useState(initialDate ?? todayISO())
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  const submit = () => {
    setError(null)
    logPeriod.mutate(
      {
        startDate: mode === 'today' ? todayISO() : date,
        notes: notes.trim() || undefined,
      },
      {
        onSuccess: () => {
          toast(t('period.saved'))
          setNotes('')
          onClose()
        },
        onError: (err) => setError(errorMessage(err)),
      },
    )
  }

  return (
    <Sheet open={open} onClose={onClose} title={t('period.logFor', { name: partnerName })}>
      <div className="space-y-4">
        <SegmentedControl
          options={[
            { value: 'today', label: t('period.startedToday') },
            { value: 'other', label: t('period.startedOn') },
          ]}
          value={mode}
          onChange={setMode}
        />
        {mode === 'other' && (
          <Field label={t('period.startDate')} htmlFor="period-date">
            <Input
              id="period-date"
              type="date"
              value={date}
              max={todayISO()}
              onChange={(e) => setDate(e.target.value)}
            />
          </Field>
        )}
        <Field
          label={`${t('period.notes')} (${t('common.optional')})`}
          htmlFor="period-notes"
          error={error ?? undefined}
        >
          <Textarea
            id="period-notes"
            value={notes}
            onChange={(e) => setNotes(e.target.value)}
            rows={2}
          />
        </Field>
        <div className="flex justify-end gap-2">
          <Button variant="secondary" onClick={onClose} disabled={logPeriod.isPending}>
            {t('common.cancel')}
          </Button>
          <Button onClick={submit} disabled={logPeriod.isPending || (mode === 'other' && !date)}>
            {logPeriod.isPending ? t('common.saving') : t('common.save')}
          </Button>
        </div>
      </div>
    </Sheet>
  )
}
