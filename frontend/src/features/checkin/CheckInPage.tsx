import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { SmilePlus } from 'lucide-react'
import {
  useCheckIns,
  useCreateCheckIn,
  useCreateObservation,
  usePartners,
} from '../../lib/queries'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { Select, Textarea } from '../../components/ui/Input'
import { Tag } from '../../components/ui/Tag'
import { useToast } from '../../components/ui/Toast'
import { moodEmoji, observationEmoji } from '../../lib/emoji'
import { errorMessage } from '../../lib/errors'
import { formatDate, todayISO } from '../../lib/dates'
import type { Mood, ObservationType } from '../../lib/types'

const MOODS: Mood[] = [
  'VERY_HAPPY',
  'HAPPY',
  'CALM',
  'NEUTRAL',
  'TIRED',
  'STRESSED',
  'SAD',
  'ANGRY',
  'ANXIOUS',
  'OVERWHELMED',
  'AFFECTIONATE',
]

const OBSERVATIONS: ObservationType[] = [
  'VERY_HAPPY',
  'HAPPY',
  'NEUTRAL',
  'TIRED',
  'STRESSED',
  'SAD',
  'UPSET',
  'DISTANT',
  'AFFECTIONATE',
  'NEEDS_SPACE',
  'NOT_SURE',
]

function LevelPicker({
  label,
  value,
  onChange,
}: {
  label: string
  value: number
  onChange: (value: number) => void
}) {
  const { t } = useTranslation()
  return (
    <div>
      <div className="flex items-baseline justify-between">
        <span className="text-[12.5px] font-medium text-ink-2">{label}</span>
        <span className="text-[11.5px] text-ink-3">{t(`checkin.levels.${value}`)}</span>
      </div>
      <div className="mt-1.5 flex gap-1.5" role="radiogroup" aria-label={label}>
        {[1, 2, 3, 4, 5].map((level) => (
          <button
            key={level}
            role="radio"
            aria-checked={value === level}
            onClick={() => onChange(level)}
            className={`h-8 flex-1 rounded-md border text-[13px] font-medium transition-colors ${
              level <= value
                ? 'border-peach bg-peach-soft text-peach'
                : 'border-border bg-surface text-ink-3 hover:border-border-strong'
            }`}
          >
            {level}
          </button>
        ))}
      </div>
    </div>
  )
}

export function CheckInPage() {
  const { t } = useTranslation()
  const toast = useToast()
  const [searchParams] = useSearchParams()
  const { data: partners, isLoading } = usePartners()
  const createCheckIn = useCreateCheckIn()
  const createObservation = useCreateObservation()

  const activePartners = useMemo(
    () => (partners ?? []).filter((p) => p.relationship?.status !== 'ENDED'),
    [partners],
  )

  const [partnerId, setPartnerId] = useState('')
  useEffect(() => {
    const fromUrl = searchParams.get('partner')
    if (fromUrl && activePartners.some((p) => p.id === fromUrl)) {
      setPartnerId(fromUrl)
    } else if (!partnerId && activePartners.length > 0) {
      setPartnerId(activePartners[0].id)
    }
  }, [searchParams, activePartners, partnerId])

  const partner = activePartners.find((p) => p.id === partnerId)
  const partnerName = partner ? (partner.nickname ?? partner.name) : ''
  const { data: checkIns } = useCheckIns(partnerId || undefined)

  const [mood, setMood] = useState<Mood | null>(null)
  const [energy, setEnergy] = useState(3)
  const [stress, setStress] = useState(3)
  const [note, setNote] = useState('')
  const [observation, setObservation] = useState<ObservationType | null>(null)
  const [observationNote, setObservationNote] = useState('')

  if (isLoading) return <PageLoader />

  if (activePartners.length === 0) {
    return (
      <div className="rounded-xl border border-border bg-surface">
        <EmptyState
          icon={<SmilePlus size={28} strokeWidth={1.5} />}
          title={t('checkin.title')}
          body={t('checkin.noPartners')}
        />
      </div>
    )
  }

  const alreadyCheckedInToday = checkIns?.some((c) => c.mine && c.date === todayISO())

  const submitCheckIn = () => {
    if (!mood || !partnerId) return
    createCheckIn.mutate(
      {
        partnerId,
        mood,
        energyLevel: energy,
        stressLevel: stress,
        note: note.trim() || undefined,
      },
      {
        onSuccess: () => {
          toast(t('checkin.saved'))
          setNote('')
        },
        onError: (err) => toast(errorMessage(err), 'error'),
      },
    )
  }

  const submitObservation = () => {
    if (!observation || !partnerId) return
    createObservation.mutate(
      { partnerId, observationType: observation, note: observationNote.trim() || undefined },
      {
        onSuccess: () => {
          toast(t('checkin.observationSaved'))
          setObservation(null)
          setObservationNote('')
        },
        onError: (err) => toast(errorMessage(err), 'error'),
      },
    )
  }

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <header className="flex items-center justify-between gap-3">
        <h1 className="font-display text-[21px] font-semibold text-ink">{t('checkin.title')}</h1>
        {activePartners.length > 1 && (
          <div className="w-44">
            <Select
              aria-label={t('checkin.withWhom')}
              value={partnerId}
              onChange={(e) => setPartnerId(e.target.value)}
            >
              {activePartners.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.nickname ?? p.name}
                </option>
              ))}
            </Select>
          </div>
        )}
      </header>

      {/* My check-in */}
      <section className="rounded-xl border border-border bg-surface p-4 sm:p-5">
        <h2 className="text-[15px] font-semibold text-ink">{t('checkin.howAreYou')}</h2>
        {alreadyCheckedInToday && (
          <p className="mt-1 text-[12.5px] text-ink-3">{t('checkin.alreadyToday')}</p>
        )}
        <div className="mt-3 grid grid-cols-3 gap-1.5 sm:grid-cols-4">
          {MOODS.map((m) => (
            <button
              key={m}
              onClick={() => setMood(m)}
              aria-pressed={mood === m}
              className={`flex flex-col items-center gap-0.5 rounded-lg border px-1 py-2 transition-colors ${
                mood === m
                  ? 'border-peach bg-peach-soft'
                  : 'border-border bg-surface hover:border-border-strong'
              }`}
            >
              <span className="text-[19px]" aria-hidden="true">{moodEmoji[m]}</span>
              <span className="text-[11.5px] font-medium text-ink-2">{t(`mood.${m}`)}</span>
            </button>
          ))}
        </div>

        <div className="mt-4 space-y-3">
          <LevelPicker label={t('checkin.energy')} value={energy} onChange={setEnergy} />
          <LevelPicker label={t('checkin.stress')} value={stress} onChange={setStress} />
        </div>

        <div className="mt-4">
          <Field label={`${t('checkin.note')} (${t('common.optional')})`} htmlFor="checkin-note">
            <Textarea
              id="checkin-note"
              placeholder={t('checkin.notePlaceholder')}
              value={note}
              onChange={(e) => setNote(e.target.value)}
              rows={2}
            />
          </Field>
        </div>

        <div className="mt-4 flex justify-end">
          <Button onClick={submitCheckIn} disabled={!mood || createCheckIn.isPending}>
            {createCheckIn.isPending ? t('common.saving') : t('checkin.submit')}
          </Button>
        </div>
      </section>

      {/* How do I perceive my partner */}
      {partner && (
        <section className="rounded-xl border border-border bg-surface p-4 sm:p-5">
          <h2 className="text-[15px] font-semibold text-ink">
            {t('checkin.howDoYouSeeHer', { name: partnerName })}
          </h2>
          <p className="mt-0.5 text-[12px] text-ink-3">{t('checkin.observationDisclaimer')}</p>
          <div className="mt-3 grid grid-cols-3 gap-1.5 sm:grid-cols-4">
            {OBSERVATIONS.map((type) => (
              <button
                key={type}
                onClick={() => setObservation(observation === type ? null : type)}
                aria-pressed={observation === type}
                className={`flex flex-col items-center gap-0.5 rounded-lg border px-1 py-2 transition-colors ${
                  observation === type
                    ? 'border-plum bg-plum-soft'
                    : 'border-border bg-surface hover:border-border-strong'
                }`}
              >
                <span className="text-[19px]" aria-hidden="true">{observationEmoji[type]}</span>
                <span className="text-[11.5px] font-medium text-ink-2">
                  {t(`observationType.${type}`)}
                </span>
              </button>
            ))}
          </div>
          {observation && (
            <div className="mt-3">
              <Field
                label={`${t('checkin.observationNote')} (${t('common.optional')})`}
                htmlFor="observation-note"
              >
                <Textarea
                  id="observation-note"
                  value={observationNote}
                  onChange={(e) => setObservationNote(e.target.value)}
                  rows={2}
                />
              </Field>
            </div>
          )}
          <div className="mt-4 flex justify-end">
            <Button
              variant="secondary"
              onClick={submitObservation}
              disabled={!observation || createObservation.isPending}
            >
              {createObservation.isPending ? t('common.saving') : t('common.save')}
            </Button>
          </div>
        </section>
      )}

      {/* Recent check-ins */}
      {checkIns && checkIns.length > 0 && (
        <section>
          <h2 className="text-[13px] font-medium uppercase tracking-wide text-ink-3">
            {t('checkin.yourRecent')}
          </h2>
          <ul className="mt-2 divide-y divide-border rounded-xl border border-border bg-surface">
            {checkIns.slice(0, 10).map((checkIn) => (
              <li key={checkIn.id} className="flex items-center gap-3 px-4 py-2.5 text-[13.5px]">
                <span className="text-[17px]" aria-hidden="true">{moodEmoji[checkIn.mood]}</span>
                <span className="font-medium text-ink">{t(`mood.${checkIn.mood}`)}</span>
                {!checkIn.mine && (
                  <Tag tone="teal">
                    {partnerName} · {t('checkin.sharedBadge')}
                  </Tag>
                )}
                {checkIn.note && (
                  <span className="min-w-0 flex-1 truncate text-ink-3">{checkIn.note}</span>
                )}
                <span className="ml-auto shrink-0 text-[12px] text-ink-3">
                  {checkIn.date === todayISO() ? t('common.today') : formatDate(checkIn.date)}
                </span>
              </li>
            ))}
          </ul>
        </section>
      )}
    </div>
  )
}
