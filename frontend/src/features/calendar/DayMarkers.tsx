type MarkerProps = {
  /** "sm" fits a day cell; "md" is for legends and day details. */
  size?: 'sm' | 'md'
  /** With a title the marker is announced; without one it is decorative. */
  title?: string
}

function a11y(title?: string) {
  return title ? { role: 'img', 'aria-label': title } : { 'aria-hidden': true as const }
}

/** A day with a logged encounter. */
export function BombMarker({ size = 'sm', title }: MarkerProps) {
  return (
    <span
      {...a11y(title)}
      className={`inline-block leading-none ${size === 'md' ? 'text-[15px]' : 'text-[10px]'}`}
    >
      💣
    </span>
  )
}

/** Estimated ovulation: a bomb sitting inside a flame. */
export function OvulationMarker({ size = 'sm', title }: MarkerProps) {
  const flame = size === 'md' ? 'text-[19px]' : 'text-[13px]'
  const bomb = size === 'md' ? 'text-[9px]' : 'text-[6.5px]'
  return (
    <span
      {...a11y(title)}
      className={`relative inline-flex items-center justify-center leading-none ${flame}`}
    >
      🔥
      <span className={`absolute inset-x-0 bottom-[16%] text-center leading-none ${bomb}`}>💣</span>
    </span>
  )
}
