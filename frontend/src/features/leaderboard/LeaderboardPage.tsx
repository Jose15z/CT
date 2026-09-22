import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Crown, Trophy } from 'lucide-react'
import { useLeaderboard, useLeaderboardMe, useUpdateLeaderboardSettings } from '../../lib/queries'
import { useAuth } from '../../lib/auth'
import { PageLoader } from '../../components/ui/Spinner'
import { EmptyState } from '../../components/ui/EmptyState'
import { SegmentedControl } from '../../components/ui/SegmentedControl'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { Input } from '../../components/ui/Input'
import { Sheet } from '../../components/ui/Sheet'
import { useToast } from '../../components/ui/Toast'
import { errorMessage } from '../../lib/errors'
import type { LeaderboardWindow } from '../../lib/types'

export function LeaderboardPage() {
  const { t } = useTranslation()
  const toast = useToast()
  const { user } = useAuth()
  const [window, setWindow] = useState<LeaderboardWindow>('GLOBAL')
  const { data: ranking, isLoading } = useLeaderboard(window)
  const { data: me } = useLeaderboardMe()
  const updateSettings = useUpdateLeaderboardSettings()

  const [settingsOpen, setSettingsOpen] = useState(false)
  const [alias, setAlias] = useState('')
  const [showAvatar, setShowAvatar] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const openSettings = () => {
    setAlias(me?.publicAlias ?? user?.displayName ?? '')
    setShowAvatar(me?.showAvatar ?? false)
    setError(null)
    setSettingsOpen(true)
  }

  const saveSettings = (enabled: boolean) => {
    setError(null)
    updateSettings.mutate(
      { enabled, publicAlias: alias.trim() || undefined, showAvatar },
      {
        onSuccess: () => {
          toast(t('leaderboard.settingsSaved'))
          setSettingsOpen(false)
        },
        onError: (err) => setError(errorMessage(err)),
      },
    )
  }

  if (isLoading) return <PageLoader />

  const medal = (rank: number) =>
    rank === 1 ? '🥇' : rank === 2 ? '🥈' : rank === 3 ? '🥉' : null

  return (
    <div className="mx-auto max-w-xl space-y-5">
      <header>
        <h1 className="flex items-center gap-2 font-display text-[21px] font-semibold text-ink">
          <Trophy size={20} className="text-warning" aria-hidden="true" />
          {t('leaderboard.title')}
        </h1>
        <p className="mt-0.5 text-[13px] text-ink-2">{t('leaderboard.subtitle')}</p>
      </header>

      <div className="flex flex-wrap items-center justify-between gap-3">
        <SegmentedControl
          ariaLabel={t('leaderboard.title')}
          options={[
            { value: 'GLOBAL', label: t('leaderboard.window.GLOBAL') },
            { value: 'MONTH', label: t('leaderboard.window.MONTH') },
            { value: 'YEAR', label: t('leaderboard.window.YEAR') },
          ]}
          value={window}
          onChange={setWindow}
        />
        <Button variant="secondary" size="sm" onClick={openSettings}>
          {me?.enabled ? t('settings.leaderboardConfig') : t('leaderboard.enable')}
        </Button>
      </div>

      {/* My status strip */}
      {me && (
        <div className="flex items-center justify-between rounded-lg border border-border bg-surface-2 px-4 py-2.5 text-[13px]">
          <span className="text-ink-2">
            {me.enabled && me.rank
              ? `${t('leaderboard.yourPosition')}: #${me.rank}`
              : t('leaderboard.notParticipating')}
          </span>
          <span className="font-semibold text-ink">
            {t('leaderboard.score', { count: me.score })}
          </span>
        </div>
      )}

      {!ranking || ranking.entries.length === 0 ? (
        <div className="rounded-xl border border-border bg-surface">
          <EmptyState
            icon={<Crown size={28} strokeWidth={1.5} />}
            title={t('leaderboard.empty.title')}
            body={t('leaderboard.empty.body')}
            action={
              !me?.enabled ? (
                <Button onClick={openSettings}>{t('leaderboard.enable')}</Button>
              ) : undefined
            }
          />
        </div>
      ) : (
        <ol className="divide-y divide-border overflow-hidden rounded-xl border border-border bg-surface">
          {ranking.entries.map((entry) => (
            <li
              key={`${entry.rank}-${entry.alias}`}
              className={`flex items-center gap-3 px-4 py-3 ${entry.me ? 'bg-peach-soft/60' : ''}`}
            >
              <span className="w-8 text-center text-[14px] font-semibold text-ink-2">
                {medal(entry.rank) ?? entry.rank}
              </span>
              <span className="flex min-w-0 flex-1 items-center gap-2">
                {entry.avatarEmoji && <span aria-hidden="true">{entry.avatarEmoji}</span>}
                <span className="truncate text-[14px] font-medium text-ink">{entry.alias}</span>
                {entry.me && (
                  <span className="rounded bg-peach px-1.5 py-0.5 text-[10.5px] font-semibold text-on-peach">
                    {t('leaderboard.you')}
                  </span>
                )}
              </span>
              <span className="text-[14px] font-semibold tabular-nums text-ink">{entry.score}</span>
            </li>
          ))}
        </ol>
      )}

      <p className="text-[11.5px] leading-relaxed text-ink-3">{t('leaderboard.privacyNote')}</p>

      {/* Settings sheet */}
      <Sheet open={settingsOpen} onClose={() => setSettingsOpen(false)} title={t('leaderboard.joinTitle')}>
        <div className="space-y-4">
          <p className="text-[13px] leading-relaxed text-ink-2">{t('leaderboard.joinBody')}</p>
          <Field label={t('leaderboard.alias')} htmlFor="lb-alias" hint={t('leaderboard.aliasHint')}>
            <Input
              id="lb-alias"
              value={alias}
              onChange={(e) => setAlias(e.target.value)}
              maxLength={30}
            />
          </Field>
          <label className="flex cursor-pointer items-center gap-2.5 text-[13.5px] text-ink">
            <input
              type="checkbox"
              checked={showAvatar}
              onChange={(e) => setShowAvatar(e.target.checked)}
              className="h-4 w-4 accent-[var(--peach)]"
            />
            {t('leaderboard.showAvatar')}
          </label>
          {error && <p className="text-[13px] text-danger">{error}</p>}
          <div className="flex flex-wrap justify-end gap-2">
            {me?.enabled && (
              <Button
                variant="danger"
                onClick={() => saveSettings(false)}
                disabled={updateSettings.isPending}
              >
                {t('leaderboard.disable')}
              </Button>
            )}
            <Button
              onClick={() => saveSettings(true)}
              disabled={updateSettings.isPending || !alias.trim()}
            >
              {me?.enabled ? t('common.save') : t('leaderboard.enable')}
            </Button>
          </div>
        </div>
      </Sheet>
    </div>
  )
}
