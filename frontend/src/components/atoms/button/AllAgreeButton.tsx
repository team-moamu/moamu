import { Input } from '../input/Input'

type AllAgreeButtonProps = {
  checked: boolean
  onChange: (checked: boolean) => void
}

export function AllAgreeButton({ checked, onChange }: AllAgreeButtonProps) {
  return (
    <label className="flex h-[var(--button-height-primary)] w-full cursor-pointer items-center gap-2 rounded-[var(--radius-md)] bg-[#C8D5C9] px-3">
      <Input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="cursor-pointer accent-[#426B45]"
      />
      <span className="text-caption">전체 동의</span>
    </label>
  )
}
