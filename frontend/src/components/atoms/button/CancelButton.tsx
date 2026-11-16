import type { ButtonHTMLAttributes } from 'react'
import { Button } from './Button'

type CancelButtonProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'className'>

export function CancelButton({ children = '취소', type = 'button', ...props }: CancelButtonProps) {
  return (
    <Button
      {...props}
      type={type}
      className="flex w-full items-center justify-center rounded-md bg-[#F4E7D4] py-3 text-sm font-semibold text-[#4B3A29] transition-colors enabled:hover:bg-[#EBDCC5] disabled:cursor-not-allowed disabled:opacity-50"
    >
      {children}
    </Button>
  )
}
