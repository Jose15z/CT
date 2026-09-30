import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import es from './es.json'
import en from './en.json'

const stored = (() => {
  try {
    return localStorage.getItem('ct.lang')
  } catch {
    return null
  }
})()

// Spanish is the product's language: it is what the static <head> promises and
// what a crawler (which reports an English browser) should render. The toggle
// persists an explicit choice.
i18n.use(initReactI18next).init({
  resources: {
    es: { translation: es },
    en: { translation: en },
  },
  lng: stored ?? 'es',
  fallbackLng: 'es',
  interpolation: { escapeValue: false },
})

// Keeps <html lang> honest for screen readers and search engines.
if (typeof document !== 'undefined') {
  document.documentElement.lang = i18n.language
  i18n.on('languageChanged', (lng) => {
    document.documentElement.lang = lng
  })
}

export function setLanguage(lang: 'es' | 'en') {
  i18n.changeLanguage(lang)
  try {
    localStorage.setItem('ct.lang', lang)
  } catch {
    // storage unavailable (private mode); language just won't persist
  }
}

export default i18n
