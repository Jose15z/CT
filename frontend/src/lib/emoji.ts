import type { Mood, ObservationType } from './types'

/**
 * Emoji as emotional CONTENT (check-in states picked by users), never as UI
 * iconography — structural icons are always Lucide.
 */
export const moodEmoji: Record<Mood, string> = {
  VERY_HAPPY: '😄',
  HAPPY: '🙂',
  CALM: '😌',
  NEUTRAL: '😐',
  TIRED: '😴',
  STRESSED: '😣',
  SAD: '😔',
  ANGRY: '😡',
  ANXIOUS: '😰',
  OVERWHELMED: '🫠',
  AFFECTIONATE: '🥰',
  CUSTOM: '💬',
}

export const observationEmoji: Record<ObservationType, string> = {
  VERY_HAPPY: '😄',
  HAPPY: '🙂',
  NEUTRAL: '😐',
  TIRED: '😴',
  STRESSED: '😣',
  SAD: '😔',
  UPSET: '😠',
  DISTANT: '🫥',
  AFFECTIONATE: '🥰',
  NEEDS_SPACE: '🌙',
  NOT_SURE: '🤷',
  OTHER: '💬',
}
