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
      className="inline-flex rounded-lg border border-border bg-surface-2 p-0.5"
    >
      {options.map((option) => (
        <button
          key={option.value}
          role="tab"
          aria-selected={option.value === value}
          onClick={() => onChange(option.value)}
          className={`rounded-md px-3 py-1.5 text-[13px] font-medium transition-[background-color,color,box-shadow] duration-150 ${
            option.value === value
              ? 'bg-surface text-ink shadow-card'
              : 'text-ink-2 hover:text-ink'
          }`}
        >
          {option.label}
        </button>
      ))}
    </div>
  )
}
