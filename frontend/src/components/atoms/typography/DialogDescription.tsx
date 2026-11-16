import type { HTMLAttributes } from 'react'

export function DialogDescription({ className = '', ...props }: HTMLAttributes<HTMLParagraphElement>) {
  return <p {...props} className={`m-0 whitespace-pre-line font-[family-name:var(--font-ui)] text-[13px] leading-5 text-[#493725] ${className}`} />
}
