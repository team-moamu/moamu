import type { ReactNode } from 'react'

type TextProps = { children: ReactNode }

export function Text({ children }: TextProps) {
  return <span className='text-accent'>{children}</span>
}
