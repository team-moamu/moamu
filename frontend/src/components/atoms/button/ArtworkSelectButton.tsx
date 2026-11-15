import { SelectionIndicator } from '../icon/SelectionIndicator'

interface ArtworkSelectButtonProps {
  selected: boolean
  label: string
  onClick: () => void
}

export function ArtworkSelectButton({ selected, label, onClick }: ArtworkSelectButtonProps) {
  return (
    <button
      type="button"
      aria-label={label}
      aria-pressed={selected}
      onClick={onClick}
      className={`absolute inset-0 z-10 cursor-pointer rounded-[9px] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#4F7652] ${selected ? 'bg-black/20' : 'bg-transparent'}`}
    >
      <span className="absolute top-2 right-2">
        <SelectionIndicator selected={selected} />
      </span>
    </button>
  )
}
