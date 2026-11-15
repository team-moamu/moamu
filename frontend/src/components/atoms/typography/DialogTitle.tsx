import type { HTMLAttributes } from 'react'
import './DialogTitle.css'

export function DialogTitle({ className = '', ...props }: HTMLAttributes<HTMLHeadingElement>) {
  return <h2 {...props} className={`dialog-title m-0 text-[24px] leading-8 text-[#493725] ${className}`} />
}
