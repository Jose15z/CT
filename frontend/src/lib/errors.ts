import i18n from '../i18n'
import { ApiError } from './api'

/** Translate a backend business-rule code, falling back to a generic message. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError && error.problem.code) {
    const key = `errors.${error.problem.code}`
    if (i18n.exists(key)) {
      return i18n.t(key)
    }
  }
  return i18n.t('common.error')
}
