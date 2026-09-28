import { describe, expect, it } from 'vitest'
import es from './es.json'
import en from './en.json'

type Tree = { [key: string]: Tree | string }

function flattenKeys(obj: Tree, prefix = ''): string[] {
  return Object.entries(obj).flatMap(([key, value]) => {
    const path = prefix ? `${prefix}.${key}` : key
    return typeof value === 'string' ? [path] : flattenKeys(value as Tree, path)
  })
}

function has(obj: Tree, path: string): boolean {
  let node: Tree | string = obj
  for (const part of path.split('.')) {
    if (typeof node === 'string' || node[part] === undefined) return false
    node = node[part]
  }
  return typeof node === 'string'
}

describe('translation bundles', () => {
  it('es and en have identical key structure', () => {
    expect(flattenKeys(en as Tree).sort()).toEqual(flattenKeys(es as Tree).sort())
  })

  // Every message key the BACKEND can emit must resolve in the frontend.
  // Rules and variant count mirror RelationshipAdviceEngine.RULES / VARIANTS.
  const adviceRules = [
    'anniversary.today',
    'anniversary.upcoming',
    'anniversary.approaching',
    'birthday.today',
    'birthday.upcoming',
    'birthday.approaching',
    'partnerReport.sad',
    'partnerReport.low',
    'partnerReport.good',
    'observation.needsSpace',
    'observation.sad',
    'observation.upset',
    'observation.distant',
    'observation.happy',
    'observation.affectionate',
    'observation.tired',
    'observation.stressed',
    'observation.notSure',
    'both.stressed',
    'both.good',
    'quietPlan',
    'self.low',
    'self.stressed',
    'self.tired',
    'self.lowSatisfaction',
    'self.affectionate',
    'self.good',
    'phase.menstruation',
  ]
  const ADVICE_VARIANTS = 3
  const backendAdviceKeys = adviceRules.flatMap((rule) =>
    Array.from({ length: ADVICE_VARIANTS }, (_, i) => `advice.${rule}.${i + 1}`),
  )

  // Fragment counts mirror TipCatalog.java (LEADS, ACTIONS_PER_TOPIC × 8, CLOSERS).
  const range = (prefix: string, n: number) =>
    Array.from({ length: n }, (_, i) => `${prefix}.${i + 1}`)
  const backendTipKeys = [
    'tips.composed',
    ...range('tips.lead', 56),
    ...range('tips.action', 80),
    ...range('tips.closer', 9),
  ]

  const backendErrorCodes = [
    'errors.auth.usernameTaken',
    'errors.auth.emailTaken',
    'errors.auth.invalidCredentials',
    'errors.auth.invalidRefreshToken',
    'errors.partner.duplicate',
    'errors.partner.rateLimit',
    'errors.relationship.monogamousConflict',
    'errors.relationship.marriageConflict',
    'errors.relationship.dates.datingAfterStart',
    'errors.relationship.dates.startAfterEngagement',
    'errors.relationship.dates.engagementAfterMarriage',
    'errors.relationship.dates.datingAfterMarriage',
    'errors.relationship.dates.endWithoutEnded',
    'errors.relationship.dates.endBeforeStart',
    'errors.period.futureStart',
    'errors.period.endBeforeStart',
    'errors.checkIn.futureDate',
    'errors.observation.futureDate',
    'errors.leaderboard.aliasRequired',
    'errors.cycle.invalidRange',
    'errors.avatar.invalidImage',
    'errors.avatar.tooLarge',
    'errors.partner.mustBeAdult',
    'errors.encounter.dateInFuture',
    'errors.encounter.dailyLimit',
    'errors.auth.invalidResetToken',
    'errors.auth.tooManyAttempts',
    'errors.user.invalidTimezone',
    'errors.invite.invalid',
    'errors.invite.selfLink',
    'errors.invite.alreadyLinked',
  ]

  // The XP endpoint emits titleKey ("xp.title.N", N capped at 10) and badges.
  const backendXpKeys = [
    ...Array.from({ length: 10 }, (_, i) => `xp.title.${i + 1}`),
    'xp.badge.firstSteps',
    'xp.badge.loyal',
    'xp.badge.exclusive',
    'xp.badge.veteran',
  ]

  const disclaimers = ['cycle.disclaimer.estimate', 'cycle.disclaimer.notContraception']

  it.each([...backendAdviceKeys, ...backendTipKeys, ...backendErrorCodes, ...backendXpKeys, ...disclaimers])(
    'resolves backend key %s in both languages',
    (key) => {
      expect(has(es as Tree, key), `missing in es.json: ${key}`).toBe(true)
      expect(has(en as Tree, key), `missing in en.json: ${key}`).toBe(true)
    },
  )
})
