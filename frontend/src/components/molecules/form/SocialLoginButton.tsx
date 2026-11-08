import type { ButtonHTMLAttributes } from 'react'
import { Button } from '../../atoms/button/Button'
import googleLogo from '../../../assets/images/social/google.svg'

type SocialLoginButtonProps =
  Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'children'>

export function SocialLoginButton({
  className = '',
  type = 'button',
  ...props
}: SocialLoginButtonProps) {
  return (
    <Button
      {...props}
      type={type}
      className={`
        flex min-h-[46px] w-full items-center justify-center
        gap-3 rounded-xl border border-[var(--color-border)]
        bg-white px-4 text-base font-light
        text-[var(--color-text)]
        focus-visible:outline-2 focus-visible:outline-offset-2
        focus-visible:outline-[var(--color-primary)]
        disabled:cursor-not-allowed
        ${className}
      `}
    >
      <img
        src={googleLogo}
        alt=""
        width={24}
        height={24}
        className="shrink-0"
      />
      <span>구글로 계속하기</span>
    </Button>
  )
}