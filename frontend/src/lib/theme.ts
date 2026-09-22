export type ThemePreference = 'light' | 'dark' | 'system'

const KEY = 'ct.theme'

export function getThemePreference(): ThemePreference {
  try {
    const stored = localStorage.getItem(KEY)
    if (stored === 'light' || stored === 'dark' || stored === 'system') return stored
  } catch {
    // ignore
  }
  return 'system'
}

export function applyTheme(preference: ThemePreference) {
  const systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches
  const dark = preference === 'dark' || (preference === 'system' && systemDark)
  document.documentElement.classList.toggle('dark', dark)
}

export function setThemePreference(preference: ThemePreference) {
  try {
    localStorage.setItem(KEY, preference)
  } catch {
    // ignore
  }
  applyTheme(preference)
}

export function initTheme() {
  applyTheme(getThemePreference())
  window
    .matchMedia('(prefers-color-scheme: dark)')
    .addEventListener('change', () => applyTheme(getThemePreference()))
}
