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

const browserLang = navigator.language?.startsWith('en') ? 'en' : 'es'

i18n.use(initReactI18next).init({
  resources: {
    es: { translation: es },
    en: { translation: en },
  },
  lng: stored ?? browserLang,
  fallbackLng: 'es',
  interpolation: { escapeValue: false },
})

export function setLanguage(lang: 'es' | 'en') {
  i18n.changeLanguage(lang)
  try {
    localStorage.setItem('ct.lang', lang)
  } catch {
    // storage unavailable (private mode); language just won't persist
  }
}

export default i18n
