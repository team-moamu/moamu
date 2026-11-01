// 초록색 공통컨포넌트

import type { ReactNode } from 'react'

interface SuccessButtonProps {
  children: ReactNode
  disabled?: boolean
  onClick?: () => void
}

export function SuccessButton ({
  children,
  disabled = false,
  onClick,
}: SuccessButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`
        w-full rounded-lg py-3
        text-sm font-semibold transition-colors
        ${
          disabled
            ? 'cursor-not-allowed bg-[#D9D9D9] text-[#888888]'
            : 'bg-[#6B8664] text-white hover:bg-[#587451]'
        }
      `}
    >
      {children}
    </button>
  )
}
