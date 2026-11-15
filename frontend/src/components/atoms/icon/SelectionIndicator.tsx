import { Icon } from './Icon'

interface SelectionIndicatorProps {
  selected: boolean
}

export function SelectionIndicator({ selected }: SelectionIndicatorProps) {
  return (
    <span aria-hidden="true" className={`flex h-6 w-6 items-center justify-center rounded-full border border-white ${selected ? 'bg-[#4F7652] text-white' : 'bg-white/80 text-transparent'}`}>
      <Icon name="check" size={18} />
    </span>
  )
}
