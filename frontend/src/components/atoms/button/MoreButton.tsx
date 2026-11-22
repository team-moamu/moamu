import type { ButtonHTMLAttributes } from 'react'
import { Icon, type IconName } from '../icon/Icon'

type MoreButtonProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'> & {
  icon?: IconName
  iconSize?: number
}

export function MoreButton({
  type = 'button',
  icon = 'more',
  iconSize = 24,
  className = '',
  'aria-label': ariaLabel = '더보기',
  ...props
}: MoreButtonProps) {
  return (
    <button
      {...props}
      type={type}
      aria-label={ariaLabel}
      className={`flex h-[32px] w-[44px] shrink-0 items-center justify-center rounded-[8px] bg-[#F2E6D2] text-[#4A382A] enabled:cursor-pointer hover:enabled:bg-[#EADBC3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#4F7652] disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
    >
      <Icon name={icon} size={iconSize} />
    </button>
  )
}
