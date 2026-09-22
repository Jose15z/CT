import { useTranslation } from 'react-i18next'
import { setLanguage } from '../../i18n'
import { useAuth } from '../../lib/auth'
import { useUpdateProfile } from '../../lib/queries'

/** ES / EN switch. Persists to the profile when logged in. */
export function LanguageToggle() {
  const { i18n } = useTranslation()
  const { user, refreshUser } = useAuth()
  const updateProfile = useUpdateProfile()
  const current = i18n.language?.startsWith('en') ? 'en' : 'es'

  const change = (lang: 'es' | 'en') => {
    if (lang === current) return
    setLanguage(lang)
    if (user) {
      updateProfile.mutate({ preferredLanguage: lang }, { onSuccess: () => refreshUser() })
    }
  }

  return (
    <div className="inline-flex overflow-hidden rounded-md border border-border text-[12px] font-medium">
      {(['es', 'en'] as const).map((lang) => (
        <button
          key={lang}
          onClick={() => change(lang)}
          aria-pressed={current === lang}
          className={`px-2.5 py-1.5 uppercase transition-colors ${
            current === lang ? 'bg-surface-2 text-ink' : 'bg-surface text-ink-3 hover:text-ink'
          }`}
        >
          {lang}
        </button>
      ))}
    </div>
  )
}
