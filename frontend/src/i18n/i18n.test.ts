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
  const backendAdviceKeys = [
    'advice.anniversary.today',
    'advice.anniversary.upcoming',
    'advice.anniversary.approaching',
    'advice.partnerReport.sad',
    'advice.partnerReport.low',
    'advice.observation.needsSpace',
    'advice.observation.sad',
    'advice.observation.upset',
    'advice.observation.distant',
    'advice.both.stressed',
    'advice.both.good',
    'advice.quietPlan',
    'advice.phase.menstruation',
  ]

  // Must match V2__daily_tips.sql exactly.
  const backendTipKeys = [
    'tips.stage.new.communication',
    'tips.stage.new.boundaries',
    'tips.stage.new.expectations',
    'tips.stage.new.consent',
    'tips.stage.developing.deeper',
    'tips.stage.developing.disagreements',
    'tips.stage.developing.newActivity',
    'tips.stage.developing.routines',
    'tips.stage.established.routine',
    'tips.stage.established.goals',
    'tips.stage.established.details',
    'tips.stage.established.deepTalk',
    'tips.stage.longTerm.growth',
    'tips.stage.longTerm.individuality',
    'tips.stage.longTerm.newExperience',
    'tips.stage.longTerm.intimacy',
    'tips.stage.veryLongTerm.memories',
    'tips.stage.veryLongTerm.changes',
    'tips.stage.veryLongTerm.projects',
    'tips.stage.veryLongTerm.curiosity',
    'tips.phase.menstruation.comfort',
    'tips.phase.menstruation.ask',
    'tips.phase.follicular.plan',
    'tips.phase.follicular.checkIn',
    'tips.phase.ovulation.estimate',
    'tips.phase.luteal.patience',
    'tips.phase.luteal.selfCare',
    'tips.general.listen',
    'tips.general.smallGesture',
    'tips.general.time',
    'tips.general.gratitude',
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
