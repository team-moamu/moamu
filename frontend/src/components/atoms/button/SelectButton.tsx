import type { ButtonHTMLAttributes } from 'react'

type SelectButtonProps = ButtonHTMLAttributes<HTMLButtonElement>

export function SelectButton({ children = '선택', type = 'button', className = '', ...props }: SelectButtonProps) {
  return (
    <button
      {...props}
      type={type}
      className={`flex h-[32px] min-w-[44px] shrink-0 items-center justify-center rounded-[8px] bg-[#F2E6D2] px-2 font-[family-name:var(--font-ui)] text-xs text-[#4A382A] enabled:cursor-pointer hover:enabled:bg-[#EADBC3] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#4F7652] disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
    >
      {children}
    </button>
  )
}
