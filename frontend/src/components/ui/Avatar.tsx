const sizes = {
  sm: 'h-8 w-8 text-[15px]',
  md: 'h-10 w-10 text-[19px]',
  lg: 'h-14 w-14 text-[27px]',
} as const

interface AvatarProps {
  emoji?: string | null
  name: string
  size?: keyof typeof sizes
  /** Profile photo (object URL). Takes precedence over emoji/initial. */
  imageUrl?: string | null
}

/** Photo if uploaded, else emoji if picked, else the person's initial. */
export function Avatar({ emoji, name, size = 'md', imageUrl }: AvatarProps) {
  return (
    <div
      aria-hidden="true"
      className={`flex shrink-0 items-center justify-center overflow-hidden rounded-full bg-peach-soft ${sizes[size]}`}
    >
      {imageUrl ? (
        <img src={imageUrl} alt="" className="h-full w-full object-cover" />
      ) : emoji ? (
        <span>{emoji}</span>
      ) : (
        <span className="font-display font-semibold text-peach">
          {name.charAt(0).toUpperCase()}
        </span>
      )}
    </div>
  )
}
