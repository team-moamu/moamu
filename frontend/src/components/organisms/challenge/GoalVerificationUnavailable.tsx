import { useId } from 'react'
import { Button } from '../../atoms/button/Button'
import { Icon } from '../../atoms/icon/Icon'
import { Text } from '../../atoms/typography/Text'

export type UnverifiableGoal = {
  goal: string
  suggestions: string[]
}

interface GoalVerificationUnavailableProps {
  verification: UnverifiableGoal
  onSelectGoal: (goal: string) => void
}

export function GoalVerificationUnavailable({
  verification,
  onSelectGoal,
}: GoalVerificationUnavailableProps) {
  const id = useId()

  return (
    <div className="flex flex-1 flex-col gap-4 px-5 pt-4 pb-[max(16px,env(safe-area-inset-bottom))] text-[var(--color-text)]">
      <section aria-labelledby={`${id}-intro`}>
        <h2 id={`${id}-intro`} className="m-0">
          <Text>사진으로 인증하기 어려운 목표예요</Text>
        </h2>
        <p className="text-body mt-1 mb-0">눈에 보이는 행동이 담기도록 목표를 조금만 바꿔 볼까요?</p>
      </section>

      <section aria-labelledby={`${id}-goal`}>
        <h2 id={`${id}-goal`} className="mt-0 mb-4"><Text>입력한 도전</Text></h2>
        <p
          aria-labelledby={`${id}-goal`}
          className="text-body m-0 rounded-[var(--radius-md)] border border-[var(--color-text)] px-3 py-3 break-words"
        >
          {verification.goal}
        </p>
      </section>

      <section aria-labelledby={`${id}-suggestions`}>
        <h2 id={`${id}-suggestions`} className="mt-0 mb-4"><Text>이렇게 바꿔보세요</Text></h2>
        <ul className="m-0 flex list-none flex-col gap-2 p-0">
          {verification.suggestions.map((goal, index) => (
            <li key={index}>
              <Button
                onClick={() => onSelectGoal(goal)}
                className="text-body flex min-h-12 w-full items-center justify-between gap-3 rounded-[var(--radius-md)] border border-[var(--color-text)] px-3 py-3 text-left transition-colors hover:bg-[#FFF8ED] active:bg-[var(--color-surface-soft)]"
              >
                <span className="min-w-0 break-words">{goal}</span>
                <Icon name="chevronRight" size={24} />
              </Button>
            </li>
          ))}
        </ul>
      </section>
    </div>
  )
}
