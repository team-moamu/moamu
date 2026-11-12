// 초록색 공통컨포넌트

import type { ReactNode } from 'react'
import { Icon, type IconName } from '../icon/Icon'

interface SuccessButtonProps {
  children: ReactNode
  icon?: IconName
  disabled?: boolean
  onClick?: React.MouseEventHandler<HTMLButtonElement>
}

export function SuccessButton({
  children,
  icon,
  disabled = false,
  onClick,
}: SuccessButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      className={`
        flex w-full items-center justify-center gap-2
        rounded-md py-3
        text-sm font-semibold transition-colors
        ${
          disabled
            ? 'cursor-not-allowed bg-[#D9D9D9] text-[#888888]'
            : 'bg-[#6B8664] text-white hover:bg-[#587451]'
        }
      `}
    >
      {icon && <Icon name={icon} size={18} />}
      {children}
    </button>
  )
}