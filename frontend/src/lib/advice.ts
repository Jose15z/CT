import i18n from '../i18n'
import type { AdviceItem } from './types'

/** Backend key for tips composed from fragments (see TipCatalog.java). */
export const COMPOSED_KEY = 'tips.composed'

/**
 * Resolves an advice item to display text. Rule-based advice is a single
 * key; composed tips carry the fragment keys (lead, action, optional closer)
 * in params, each resolved with the remaining params (the partner's name).
 */
export function adviceText(item: AdviceItem): string {
  if (item.messageKey !== COMPOSED_KEY) {
    return i18n.t(item.messageKey, item.params)
  }
  const { lead, action, closer, ...rest } = item.params
  return [lead, action, closer]
    .filter((key): key is string => typeof key === 'string' && key.length > 0)
    .map((key) => i18n.t(key, rest))
    .join(' ')
}
