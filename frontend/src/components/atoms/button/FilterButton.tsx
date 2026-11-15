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
      className={`inline-flex h-[26px] w-[85px] shrink-0 items-center justify-center cursor-pointer rounded-full border border-[var(--color-text)] p-0 font-[family-name:var(--font-ui)] text-[12px] font-normal leading-none whitespace-nowrap transition-colors ${
        selected
          ? 'bg-[var(--color-text)] text-[var(--color-background)]'
          : 'bg-transparent text-[var(--color-text)]'
      } ${className}`}
    >
      {children}
    </button>
  )
}
