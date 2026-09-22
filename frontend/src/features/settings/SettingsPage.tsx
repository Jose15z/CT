import { useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { Camera, ChevronRight, LogOut, ShieldCheck, Trash2, Trophy } from 'lucide-react'
import { useAuth } from '../../lib/auth'
import {
  useChangePassword,
  useDeleteAvatar,
  useMyAvatar,
  useUpdateProfile,
  useUploadAvatar,
} from '../../lib/queries'
import { Avatar } from '../../components/ui/Avatar'
import { Button } from '../../components/ui/Button'
import { Field } from '../../components/ui/Field'
import { Input, Select } from '../../components/ui/Input'
import { SegmentedControl } from '../../components/ui/SegmentedControl'
import { useToast } from '../../components/ui/Toast'
import { LanguageToggle } from './LanguageToggle'
import { errorMessage } from '../../lib/errors'
import { getThemePreference, setThemePreference } from '../../lib/theme'
import type { ThemePreference } from '../../lib/theme'
import type { RelationshipSituation } from '../../lib/types'

const SITUATIONS: RelationshipSituation[] = [
  'SINGLE',
  'IN_RELATIONSHIP',
  'MARRIED',
  'POLYAMOROUS',
  'OTHER',
]

export function SettingsPage() {
  const { t } = useTranslation()
  const toast = useToast()
  const navigate = useNavigate()
  const { user, logout, refreshUser } = useAuth()
  const updateProfile = useUpdateProfile()
  const changePassword = useChangePassword()
  const { data: avatarUrl } = useMyAvatar(!!user?.hasAvatar)
  const uploadAvatar = useUploadAvatar()
  const deleteAvatar = useDeleteAvatar()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [displayName, setDisplayName] = useState(user?.displayName ?? '')
  const [avatarEmoji, setAvatarEmoji] = useState(user?.avatarEmoji ?? '')
  const [situation, setSituation] = useState<RelationshipSituation>(
    user?.relationshipSituation ?? 'SINGLE',
  )
  const [theme, setTheme] = useState<ThemePreference>(getThemePreference())
  const [passwords, setPasswords] = useState({ current: '', next: '' })
  const [passwordError, setPasswordError] = useState<string | null>(null)

  if (!user) return null

  const saveProfile = () => {
    updateProfile.mutate(
      {
        displayName: displayName.trim(),
        avatarEmoji,
        relationshipSituation: situation,
      },
      {
        onSuccess: async () => {
          await refreshUser()
          toast(t('settings.profileSaved'))
        },
        onError: (err) => toast(errorMessage(err), 'error'),
      },
    )
  }

  const submitPassword = () => {
    setPasswordError(null)
    changePassword.mutate(
      { currentPassword: passwords.current, newPassword: passwords.next },
      {
        onSuccess: () => {
          toast(t('settings.passwordChanged'))
          setPasswords({ current: '', next: '' })
        },
        onError: (err) => setPasswordError(errorMessage(err)),
      },
    )
  }

  const changeTheme = (preference: ThemePreference) => {
    setTheme(preference)
    setThemePreference(preference)
  }

  return (
    <div className="mx-auto max-w-xl space-y-6">
      <h1 className="font-display text-[21px] font-semibold text-ink">{t('settings.title')}</h1>

      {/* Profile */}
      <section className="rounded-xl border border-border bg-surface p-4 sm:p-5">
        <h2 className="text-[15px] font-semibold text-ink">{t('settings.profile')}</h2>

        {/* Profile photo */}
        <div className="mt-3 flex items-center gap-4">
          <Avatar
            emoji={user.avatarEmoji}
            name={user.displayName}
            size="lg"
            imageUrl={avatarUrl}
          />
          <div className="min-w-0 flex-1">
            <div className="flex flex-wrap gap-2">
              <Button
                variant="secondary"
                size="sm"
                icon={<Camera size={13} />}
                disabled={uploadAvatar.isPending}
                onClick={() => fileInputRef.current?.click()}
              >
                {uploadAvatar.isPending ? t('common.saving') : t('settings.avatarUpload')}
              </Button>
              {user.hasAvatar && (
                <Button
                  variant="ghost"
                  size="sm"
                  icon={<Trash2 size={13} />}
                  disabled={deleteAvatar.isPending}
                  onClick={() =>
                    deleteAvatar.mutate(undefined, {
                      onSuccess: async () => {
                        await refreshUser()
                        toast(t('settings.avatarRemoved'))
                      },
                    })
                  }
                >
                  {t('settings.avatarRemove')}
                </Button>
              )}
            </div>
            <p className="mt-1 text-[12px] text-ink-3">{t('settings.avatarHint')}</p>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/jpeg,image/png"
              className="hidden"
              onChange={(e) => {
                const file = e.target.files?.[0]
                e.target.value = ''
                if (!file) return
                uploadAvatar.mutate(file, {
                  onSuccess: async () => {
                    await refreshUser()
                    toast(t('settings.avatarSaved'))
                  },
                  onError: (err) => toast(errorMessage(err), 'error'),
                })
              }}
            />
          </div>
        </div>

        <div className="mt-4 space-y-4 border-t border-border pt-4">
          <div className="grid grid-cols-[1fr_90px] gap-3">
            <Field label={t('auth.displayName')} htmlFor="settings-name">
              <Input
                id="settings-name"
                value={displayName}
                onChange={(e) => setDisplayName(e.target.value)}
                maxLength={60}
              />
            </Field>
            <Field label={t('partner.avatar')} htmlFor="settings-avatar">
              <Input
                id="settings-avatar"
                value={avatarEmoji}
                onChange={(e) => setAvatarEmoji(e.target.value)}
                maxLength={4}
              />
            </Field>
          </div>
          <Field label={t('situation.label')} htmlFor="settings-situation">
            <Select
              id="settings-situation"
              value={situation}
              onChange={(e) => setSituation(e.target.value as RelationshipSituation)}
            >
              {SITUATIONS.map((s) => (
                <option key={s} value={s}>
                  {t(`situation.${s}`)}
                </option>
              ))}
            </Select>
          </Field>
          <div className="flex justify-end">
            <Button
              onClick={saveProfile}
              disabled={updateProfile.isPending || !displayName.trim()}
            >
              {updateProfile.isPending ? t('common.saving') : t('common.save')}
            </Button>
          </div>
        </div>
      </section>

      {/* Appearance & language */}
      <section className="rounded-xl border border-border bg-surface p-4 sm:p-5">
        <div className="flex items-center justify-between">
          <h2 className="text-[15px] font-semibold text-ink">{t('settings.language')}</h2>
          <LanguageToggle />
        </div>
        <div className="mt-4 flex items-center justify-between border-t border-border pt-4">
          <h2 className="text-[15px] font-semibold text-ink">{t('settings.theme')}</h2>
          <SegmentedControl
            ariaLabel={t('settings.theme')}
            options={[
              { value: 'light', label: t('settings.themeLight') },
              { value: 'dark', label: t('settings.themeDark') },
              { value: 'system', label: t('settings.themeSystem') },
            ]}
            value={theme}
            onChange={changeTheme}
          />
        </div>
      </section>

      {/* Links: leaderboard config + privacy */}
      <section className="divide-y divide-border rounded-xl border border-border bg-surface">
        <Link
          to="/leaderboard"
          className="flex items-center gap-3 px-4 py-3.5 hover:bg-surface-2 sm:px-5"
        >
          <Trophy size={16} className="text-ink-3" aria-hidden="true" />
          <span className="flex-1 text-[14px] font-medium text-ink">
            {t('settings.leaderboardConfig')}
          </span>
          <ChevronRight size={15} className="text-ink-3" aria-hidden="true" />
        </Link>
        <Link
          to="/privacy"
          className="flex items-center gap-3 px-4 py-3.5 hover:bg-surface-2 sm:px-5"
        >
          <ShieldCheck size={16} className="text-ink-3" aria-hidden="true" />
          <span className="flex-1 text-[14px] font-medium text-ink">
            {t('settings.privacyLink')}
          </span>
          <ChevronRight size={15} className="text-ink-3" aria-hidden="true" />
        </Link>
      </section>

      {/* Password */}
      <section className="rounded-xl border border-border bg-surface p-4 sm:p-5">
        <h2 className="text-[15px] font-semibold text-ink">{t('settings.password')}</h2>
        <div className="mt-3 space-y-4">
          <Field label={t('settings.currentPassword')} htmlFor="settings-current-password">
            <Input
              id="settings-current-password"
              type="password"
              autoComplete="current-password"
              value={passwords.current}
              onChange={(e) => setPasswords((p) => ({ ...p, current: e.target.value }))}
            />
          </Field>
          <Field
            label={t('settings.newPassword')}
            htmlFor="settings-new-password"
            hint={t('auth.passwordHint')}
            error={passwordError ?? undefined}
          >
            <Input
              id="settings-new-password"
              type="password"
              autoComplete="new-password"
              value={passwords.next}
              onChange={(e) => setPasswords((p) => ({ ...p, next: e.target.value }))}
            />
          </Field>
          <div className="flex justify-end">
            <Button
              variant="secondary"
              onClick={submitPassword}
              disabled={changePassword.isPending || !passwords.current || passwords.next.length < 8}
            >
              {changePassword.isPending ? t('common.saving') : t('settings.password')}
            </Button>
          </div>
        </div>
      </section>

      {/* Account */}
      <section className="flex items-center justify-between rounded-xl border border-border bg-surface px-4 py-3.5 sm:px-5">
        <div>
          <p className="text-[14px] font-medium text-ink">{user.displayName}</p>
          <p className="text-[12.5px] text-ink-3">{user.email}</p>
        </div>
        <Button
          variant="secondary"
          size="sm"
          icon={<LogOut size={13} />}
          onClick={async () => {
            await logout()
            navigate('/login')
          }}
        >
          {t('nav.logout')}
        </Button>
      </section>
    </div>
  )
}
