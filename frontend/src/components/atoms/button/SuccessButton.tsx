// 초록색 공통컨포넌트

import type { ButtonHTMLAttributes, ReactNode } from 'react'
import { Button } from './Button'
import { Icon, type IconName } from '../icon/Icon'

interface SuccessButtonProps {
  children: ReactNode
  icon?: IconName
  disabled?: boolean
  disabledTone?: 'neutral' | 'blue'
  onClick?: React.MouseEventHandler<HTMLButtonElement>
  type?: ButtonHTMLAttributes<HTMLButtonElement>['type']
}

export function SuccessButton({
  children,
  icon,
  disabled = false,
  disabledTone = 'neutral',
  onClick,
  type = 'button',
}: SuccessButtonProps) {
  const disabledColors = disabledTone === 'blue'
    ? 'bg-[#82a9c9] text-white'
    : 'bg-[#D9D9D9] text-[#888888]'

  return (
    <Button
      type={type}
      onClick={onClick}
      disabled={disabled}
      className={`
        flex w-full items-center justify-center gap-2
        rounded-md py-3
        text-sm font-semibold transition-colors
        ${
          disabled
            ? `cursor-not-allowed ${disabledColors}`
            : 'bg-[#6B8664] text-white hover:bg-[#587451]'
        }
      `}
    >
      {icon && <Icon name={icon} size={18} />}
      {children}
    </Button>
  )
}
