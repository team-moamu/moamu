
import { Icon } from '../icon/Icon'
import type { IconName } from '../icon/Icon'

interface GoalCardProps {
  title: string
  description: string
  icon: IconName
  selected: boolean
  onClick: () => void
}

export function GoalCard({
  title,
  description,
  icon,
  selected,
  onClick,
}: GoalCardProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-pressed={selected}
      className={`
        flex w-full items-center gap-3 rounded-lg
        border p-3 text-left transition-colors
        ${
          selected
            ? 'border-[#6B8664] bg-[#DFE8D7]'
            : 'border-transparent bg-[#F4E7D4]'
        }
      `}
    >
      <Icon
        name={icon}
        size={32}
        className="text-[#4B3A29]"
      />

      <div className="flex min-w-0 flex-col gap-1">
        <span className="text-sm font-semibold text-[#4B3A29]">
          {title}
        </span>
        <span className="text-xs text-[#6B5B4B]">
          {description}
        </span>
      </div>
    </button>
  )
}
