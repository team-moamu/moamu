import type { ButtonHTMLAttributes } from 'react'

type FilterButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  selected: boolean
}

export function FilterButton({ selected, children, className = '', ...props }: FilterButtonProps) {
  return (
    <button
      {...props}
      type="button"
      aria-pressed={selected}
      className={`cursor-pointer rounded-full border border-[var(--color-text)] px-4 py-1 text-sm leading-5 whitespace-nowrap transition-colors ${
        selected
          ? 'bg-[var(--color-text)] text-[var(--color-background)]'
          : 'bg-transparent text-[var(--color-text)]'
      } ${className}`}
    >
      {children}
    </button>
  )
}
