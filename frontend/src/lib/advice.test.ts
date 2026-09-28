import { beforeAll, describe, expect, it } from 'vitest'
import i18n from '../i18n'
import es from '../i18n/es.json'
import { adviceText } from './advice'

describe('adviceText', () => {
  beforeAll(async () => {
    await i18n.changeLanguage('es')
  })

  it('resolves rule-based advice with its params', () => {
    const text = adviceText({
      category: 'DATE_IDEA',
      messageKey: 'advice.self.good.1',
      params: { name: 'Laura' },
      source: 'SELF_REPORT',
    })
    expect(text).toBe(es.advice.self.good['1'].replace('{{name}}', 'Laura'))
  })

  it('joins composed fragments and interpolates the name into each', () => {
    const text = adviceText({
      category: 'SUPPORT',
      messageKey: 'tips.composed',
      params: { lead: 'tips.lead.52', action: 'tips.action.71', closer: 'tips.closer.4', name: 'Laura' },
      source: 'CYCLE',
    })
    expect(text).toBe(
      `${es.tips.lead['52'].replace('{{name}}', 'Laura')} ${es.tips.action['71']} ${es.tips.closer['4']}`,
    )
  })

  it('omits the closer when the backend sent none', () => {
    const text = adviceText({
      category: 'COMMUNICATION',
      messageKey: 'tips.composed',
      params: { lead: 'tips.lead.1', action: 'tips.action.2', name: 'Laura' },
      source: 'RELATIONSHIP',
    })
    expect(text).toBe(`${es.tips.lead['1']} ${es.tips.action['2']}`)
    expect(text.endsWith(' ')).toBe(false)
  })
})
