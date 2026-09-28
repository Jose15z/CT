import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'md' | 'sm'

const variants: Record<Variant, string> = {
  primary:
    'bg-peach text-on-peach shadow-[inset_0_1px_0_rgb(255_255_255/0.14),0_1px_2px_rgb(23_20_17/0.12)] hover:bg-peach-strong disabled:hover:bg-peach',
  secondary:
    'border border-border-strong bg-surface text-ink shadow-card hover:bg-surface-2',
  ghost: 'text-ink-2 hover:bg-surface-2 hover:text-ink',
  danger: 'border border-danger/30 bg-surface text-danger hover:bg-danger-soft',
}

const sizes: Record<Size, string> = {
  md: 'h-9 px-3.5 text-[13.5px]',
  sm: 'h-8 px-2.5 text-[13px]',
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  icon?: ReactNode
}

export function Button({
  variant = 'primary',
  size = 'md',
  icon,
  className = '',
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-1.5 rounded-lg font-medium transition-[background-color,color,box-shadow,transform] duration-150 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:active:scale-100 ${variants[variant]} ${sizes[size]} ${className}`}
      {...props}
    >
      {icon}
      {children}
    </button>
  )
}
