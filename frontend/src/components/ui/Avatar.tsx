const sizes = {
  sm: 'h-8 w-8 text-[15px]',
  md: 'h-10 w-10 text-[19px]',
  lg: 'h-14 w-14 text-[27px]',
} as const

interface AvatarProps {
  emoji?: string | null
  name: string
  size?: keyof typeof sizes
}

/** Emoji if the person picked one, otherwise their initial. */
export function Avatar({ emoji, name, size = 'md' }: AvatarProps) {
  return (
    <div
      aria-hidden="true"
      className={`flex shrink-0 items-center justify-center rounded-full bg-peach-soft ${sizes[size]}`}
    >
      {emoji ? (
        <span>{emoji}</span>
      ) : (
        <span className="font-display font-semibold text-peach">
          {name.charAt(0).toUpperCase()}
        </span>
      )}
    </div>
  )
}
