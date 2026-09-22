import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Droplet, Pencil, Trash2 } from 'lucide-react'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { Input } from '../../components/ui/Input'
import { Sheet } from '../../components/ui/Sheet'
import { Tag } from '../../components/ui/Tag'
import { ConfirmDialog } from '../../components/ui/ConfirmDialog'
import { useToast } from '../../components/ui/Toast'
import {
  useCycleProfile,
  useDeletePeriod,
  usePeriods,
  usePredictions,
  useUpdateCycleProfile,
} from '../../lib/queries'
import { errorMessage } from '../../lib/errors'
import { formatDate, formatDayMonth, toISODate } from '../../lib/dates'
import { LogPeriodSheet } from '../calendar/LogPeriodSheet'

interface CycleSectionProps {
  partnerId: string
  partnerName: string
}

export function CycleSection({ partnerId, partnerName }: CycleSectionProps) {
  const { t } = useTranslation()
  const toast = useToast()
  const { data: profile } = useCycleProfile(partnerId)
  const { data: periods } = usePeriods(partnerId)
  const updateProfile = useUpdateCycleProfile(partnerId)
  const deletePeriod = useDeletePeriod(partnerId)

  const today = new Date()
  const from = toISODate(new Date(today.getFullYear(), today.getMonth(), 1))
  const to = toISODate(new Date(today.getFullYear(), today.getMonth() + 1, 0))
  const { data: predictions } = usePredictions(partnerId, from, to)

  const [configOpen, setConfigOpen] = useState(false)
  const [logOpen, setLogOpen] = useState(false)
  const [deleteId, setDeleteId] = useState<string | null>(null)
  const [config, setConfig] = useState({ cycleLength: '28', periodLength: '5' })
  const [configError, setConfigError] = useState<string | null>(null)

  const openConfig = () => {
    setConfig({
      cycleLength: String(profile?.averageCycleLength ?? 28),
      periodLength: String(profile?.averagePeriodLength ?? 5),
    })
    setConfigError(null)
    setConfigOpen(true)
  }

  const saveConfig = () => {
    setConfigError(null)
    updateProfile.mutate(
      {
        averageCycleLength: Number(config.cycleLength),
        averagePeriodLength: Number(config.periodLength),
      },
      {
        onSuccess: () => {
          toast(t('common.saved'))
          setConfigOpen(false)
        },
        onError: (err) => setConfigError(errorMessage(err)),
      },
    )
  }

  const toggleTracking = (enabled: boolean) => {
    updateProfile.mutate({ trackingEnabled: enabled })
  }

  return (
    <section>
      <div className="flex items-center justify-between">
        <h2 className="text-[15px] font-semibold text-ink">{t('partner.detail.cycle')}</h2>
        <div className="flex gap-1">
          <Button variant="ghost" size="sm" icon={<Pencil size={13} />} onClick={openConfig}>
            {t('common.edit')}
          </Button>
          <Button size="sm" icon={<Droplet size={13} />} onClick={() => setLogOpen(true)}>
            {t('period.log')}
          </Button>
        </div>
      </div>

      {profile && !profile.trackingEnabled ? (
        <p className="mt-2 text-[13.5px] text-ink-3">{t('cycle.trackingDisabledNote')}</p>
      ) : predictions && !predictions.insufficientData ? (
        <div className="mt-2 space-y-1 text-[13.5px]">
          <div className="flex flex-wrap items-center gap-2">
            {predictions.currentPhase && (
              <Tag
                tone={
                  predictions.currentPhase === 'MENSTRUATION'
                    ? 'rose'
                    : predictions.currentPhase === 'OVULATION'
                      ? 'teal'
                      : 'neutral'
                }
              >
                {t(`phase.${predictions.currentPhase}.name`)}
              </Tag>
            )}
            {predictions.currentCycleDay && (
              <span className="text-ink-3">
                {t('cycle.cycleDayN', { day: predictions.currentCycleDay })}
              </span>
            )}
            {predictions.cycleLength && (
              <span className="text-ink-3">
                · {t('cycle.cycleLengthUsed', { days: predictions.cycleLength })}
              </span>
            )}
          </div>
          {predictions.currentPhase && (
            <p className="text-ink-2">{t(`phase.${predictions.currentPhase}.desc`)}</p>
          )}
          {predictions.nextPeriodStart && (
            <p className="text-ink-2">
              {t('cycle.nextPeriod')}:{' '}
              <span className="font-medium text-ink">
                {formatDayMonth(predictions.nextPeriodStart)}
              </span>
            </p>
          )}
          {predictions.ovulationDate && (
            <p className="text-ink-2">
              {t('cycle.ovulationEstimated')}: {formatDayMonth(predictions.ovulationDate)}
            </p>
          )}
          {predictions.fertileWindowStart && predictions.fertileWindowEnd && (
            <p className="text-ink-2">
              {t('cycle.fertileWindow')}: {formatDayMonth(predictions.fertileWindowStart)} –{' '}
              {formatDayMonth(predictions.fertileWindowEnd)}
            </p>
          )}
          <p className="pt-1 text-[11.5px] leading-snug text-ink-3">
            {t('cycle.disclaimer.estimate')} {t('cycle.disclaimer.notContraception')}
          </p>
        </div>
      ) : (
        <div className="mt-2">
          <p className="text-[14px] font-medium text-ink">{t('cycle.insufficient.title')}</p>
          <p className="text-[13px] text-ink-2">{t('cycle.insufficient.body')}</p>
        </div>
      )}

      {/* Period history */}
      {periods && periods.length > 0 && (
        <div className="mt-4">
          <h3 className="text-[12.5px] font-medium uppercase tracking-wide text-ink-3">
            {t('period.history')}
          </h3>
          <ul className="mt-1.5 divide-y divide-border border-t border-border">
            {periods.map((period) => (
              <li key={period.id} className="flex items-center gap-3 py-2 text-[13.5px]">
                <Droplet size={13} className="shrink-0 text-rose" aria-hidden="true" />
                <span className="text-ink">
                  {formatDate(period.startDate)}
                  {period.endDate
                    ? ` – ${formatDate(period.endDate)}`
                    : ` · ${t('period.ongoing')}`}
                </span>
                {period.notes && (
                  <span className="min-w-0 flex-1 truncate text-ink-3">{period.notes}</span>
                )}
                <button
                  onClick={() => setDeleteId(period.id)}
                  aria-label={t('common.delete')}
                  className="ml-auto rounded-md p-1.5 text-ink-3 hover:bg-danger-soft hover:text-danger"
                >
                  <Trash2 size={14} />
                </button>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* Cycle config sheet */}
      <Sheet open={configOpen} onClose={() => setConfigOpen(false)} title={t('cycle.config')}>
        <div className="space-y-4">
          <div className="grid grid-cols-2 gap-3">
            <Field
              label={t('cycle.avgCycleLength')}
              htmlFor="cycle-length"
              hint={t('cycle.avgCycleLengthHint')}
            >
              <Input
                id="cycle-length"
                type="number"
                min={15}
                max={60}
                value={config.cycleLength}
                onChange={(e) => setConfig((c) => ({ ...c, cycleLength: e.target.value }))}
              />
            </Field>
            <Field label={t('cycle.avgPeriodLength')} htmlFor="period-length">
              <Input
                id="period-length"
                type="number"
                min={1}
                max={12}
                value={config.periodLength}
                onChange={(e) => setConfig((c) => ({ ...c, periodLength: e.target.value }))}
              />
            </Field>
          </div>
          <label className="flex cursor-pointer items-center gap-2.5 text-[13.5px] text-ink">
            <input
              type="checkbox"
              checked={profile?.trackingEnabled ?? true}
              onChange={(e) => toggleTracking(e.target.checked)}
              className="h-4 w-4 accent-[var(--peach)]"
            />
            {t('cycle.trackingEnabled')}
          </label>
          {configError && <p className="text-[13px] text-danger">{configError}</p>}
          <div className="flex justify-end gap-2">
            <Button variant="secondary" onClick={() => setConfigOpen(false)}>
              {t('common.cancel')}
            </Button>
            <Button onClick={saveConfig} disabled={updateProfile.isPending}>
              {updateProfile.isPending ? t('common.saving') : t('common.save')}
            </Button>
          </div>
        </div>
      </Sheet>

      <LogPeriodSheet
        open={logOpen}
        onClose={() => setLogOpen(false)}
        partnerId={partnerId}
        partnerName={partnerName}
      />

      <ConfirmDialog
        open={deleteId !== null}
        title={t('period.deleteConfirm')}
        body={t('period.deleteConfirmBody')}
        danger
        busy={deletePeriod.isPending}
        onConfirm={() => {
          if (deleteId) {
            deletePeriod.mutate(deleteId, { onSuccess: () => setDeleteId(null) })
          }
        }}
        onClose={() => setDeleteId(null)}
      />
    </section>
  )
}
