
import { GoalCard } from '../../atoms/card/GoalCard'
import type { IconName } from '../../atoms/icon/Icon'

interface Goal {
  goalTemplateId: number
  title: string
  description: string
  icon: IconName
}

interface GoalCardListProps {
  goals: Goal[]
  value: number | null
  onChange: (goalTemplateId: number) => void
}

export function GoalCardList({
  goals,
  value,
  onChange,
}: GoalCardListProps) {
  return (
    <div className="grid grid-cols-2 gap-2">
      {goals.map((goal) => (
        <GoalCard
          key={goal.goalTemplateId}
          title={goal.title}
          description={goal.description}
          icon={goal.icon}
          selected={value === goal.goalTemplateId}
          onClick={() => onChange(goal.goalTemplateId)}
        />
      ))}
    </div>
  )
}
