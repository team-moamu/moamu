import { Button } from './Button'

type AgreeContinueButtonProps = {
  disabled: boolean
  onClick: () => void
  children: React.ReactNode
}

export function AgreeContinueButton({ disabled, onClick, children }: AgreeContinueButtonProps) {
  return (
    <Button
      disabled={disabled}
      onClick={onClick}
      className="flex h-[var(--button-height-primary)] w-full items-center justify-center rounded-[var(--radius-md)] bg-[#426B45] px-3 text-body font-normal text-white enabled:cursor-pointer disabled:cursor-not-allowed disabled:bg-[#E5E5E5] disabled:text-[#6B5B4B]"
    >
      {children}
    </Button>
  )
}
