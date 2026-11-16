import type { HTMLAttributes } from 'react'

type DescriptionProps = HTMLAttributes<HTMLParagraphElement>

export function Description({ children, className = '', ...props }: DescriptionProps) {
  return <p {...props} className={`text-caption ${className}`}>{children}</p>
}
