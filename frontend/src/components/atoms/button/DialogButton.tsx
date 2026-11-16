import type { ButtonHTMLAttributes } from 'react'

type DialogButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & { variant?: 'confirm' | 'cancel' }

export function DialogButton({ variant = 'confirm', className = '', type = 'button', ...props }: DialogButtonProps) {
  return <button {...props} type={type} className={`min-h-[48px] min-w-0 flex-1 rounded-[12px] border px-4 py-3 font-[family-name:var(--font-ui)] text-sm enabled:cursor-pointer disabled:cursor-not-allowed disabled:opacity-50 ${variant === 'cancel' ? 'border-[#B86B50] bg-white text-[#B86B50] hover:enabled:bg-[#FFF8EC]' : 'border-[#426B45] bg-[#426B45] text-white hover:enabled:bg-[#365939]'} ${className}`} />
}
