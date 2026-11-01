
interface DurationButtonProps {
  days: number
  selected: boolean
  onClick: () => void
}

export function DurationButton({
  days,
  selected,
  onClick,
}: DurationButtonProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={`
        flex-1 rounded-lg border py-3
        text-sm font-medium transition-colors
        ${
          selected
            ? 'border-[#6B8664] bg-[#DFE8D7] text-[#4B3A29]'
            : 'border-transparent bg-[#F4E7D4] text-[#6B5B4B]'
        }
      `}
    >
      {days}일
    </button>
  )
}
