import type { InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { ChevronDown } from 'lucide-react'

const base =
  'w-full rounded-lg border border-border-strong bg-surface px-3 text-[14px] text-ink placeholder:text-ink-3 transition-[border-color,box-shadow] duration-150 focus:border-peach focus:outline-none focus:ring-3 focus:ring-(--ring) disabled:opacity-50'

export function Input({ className = '', ...props }: InputHTMLAttributes<HTMLInputElement>) {
  return <input className={`h-9 ${base} ${className}`} {...props} />
}

export function Select({ className = '', children, ...props }: SelectHTMLAttributes<HTMLSelectElement>) {
  return (
    <div className="relative">
      <select className={`h-9 ${base} appearance-none pr-8 ${className}`} {...props}>
        {children}
      </select>
      <ChevronDown
        size={14}
        aria-hidden="true"
        className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2 text-ink-3"
      />
    </div>
  )
}

export function Textarea({ className = '', ...props }: TextareaHTMLAttributes<HTMLTextAreaElement>) {
  return <textarea className={`${base} min-h-20 py-2 ${className}`} {...props} />
}
