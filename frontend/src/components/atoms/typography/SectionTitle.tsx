import type { ReactNode } from 'react'

type SectionTitleProps = {
  children: ReactNode
  className?: string
}

export function SectionTitle({ children, className = '' }: SectionTitleProps) {
  return (
    <h2 className={`m-0 font-[family-name:var(--font-ui)] text-[20px] font-semibold leading-7 text-[var(--color-text)] ${className}`}>
      {children}
    </h2>
  )
}
