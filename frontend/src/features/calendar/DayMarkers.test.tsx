import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { BombMarker, OvulationMarker } from './DayMarkers'

describe('day markers', () => {
  it('renders the bomb for an encounter', () => {
    render(<BombMarker title="Encuentro" />)
    expect(screen.getByRole('img', { name: 'Encuentro' })).toHaveTextContent('💣')
  })

  it('nests the bomb inside the flame for ovulation', () => {
    render(<OvulationMarker title="Ovulación" />)
    const marker = screen.getByRole('img', { name: 'Ovulación' })
    expect(marker).toHaveTextContent('🔥')
    expect(marker.querySelector('span')).toHaveTextContent('💣')
  })

  it('is decorative without a title', () => {
    const { container } = render(<BombMarker />)
    expect(container.firstChild).toHaveAttribute('aria-hidden', 'true')
  })
})
