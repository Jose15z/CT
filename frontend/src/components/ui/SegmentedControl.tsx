interface SegmentedControlProps<T extends string> {
  options: { value: T; label: string }[]
  value: T
  onChange: (value: T) => void
  ariaLabel?: string
}

export function SegmentedControl<T extends string>({
  options,
  value,
  onChange,
  ariaLabel,
}: SegmentedControlProps<T>) {
  return (
    <div
      role="tablist"
      aria-label={ariaLabel}
      className="inline-flex rounded-md border border-border bg-surface-2 p-0.5"
    >
      {options.map((option) => (
        <button
          key={option.value}
          role="tab"
          aria-selected={option.value === value}
          onClick={() => onChange(option.value)}
          className={`rounded-[5px] px-3 py-1.5 text-[13px] font-medium transition-colors ${
            option.value === value
              ? 'bg-surface text-ink shadow-sm'
              : 'text-ink-2 hover:text-ink'
          }`}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
