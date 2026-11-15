import { useId } from 'react'
import { SuccessButton } from '../../atoms/button/successButton'
import { Icon } from '../../atoms/icon/Icon'
import { Text } from '../../atoms/typography/Text'

export type GoalVerification = {
  goal: string
  conditions: string[]
  photoGuide: string
}

interface GoalVerificationConditionsProps {
  verification: GoalVerification
  onRetry: () => void
  onAccept: () => void
}

export function GoalVerificationConditions({
  verification,
  onRetry,
  onAccept,
}: GoalVerificationConditionsProps) {
  const id = useId()

  return (
    <div className="flex flex-1 flex-col px-5 pt-4 pb-[max(16px,env(safe-area-inset-bottom))] text-[var(--color-text)]">
      <div className="space-y-6">
        <section aria-labelledby={`${id}-intro`}>
          <h2 id={`${id}-intro`} className="m-0">
            <Text>이렇게 실천해 볼까요?</Text>
          </h2>
          <p className="text-body mt-1 mb-4">눈에 보이는 행동으로 실천하고 사진으로 기록해요.</p>
          <h2 id={`${id}-goal`} className="mb-4"><Text>입력한 도전</Text></h2>
          <p
            aria-labelledby={`${id}-goal`}
            className="text-body m-0 rounded-[var(--radius-md)] border border-[var(--color-text)] px-3 py-3 break-words"
          >
            {verification.goal}
          </p>
        </section>

        <section aria-labelledby={`${id}-conditions`}>
          <h2 id={`${id}-conditions`} className="mb-4"><Text>인증 필수 조건</Text></h2>
          <ul className="text-body m-0 list-disc space-y-1 rounded-[var(--radius-md)] bg-[#FFF8ED] py-3 pr-4 pl-9 break-words">
            {verification.conditions.map((condition, index) => (
              <li key={index}>{condition}</li>
            ))}
          </ul>
        </section>

        <section aria-labelledby={`${id}-guide`}>
          <h2 id={`${id}-guide`} className="mb-4 flex items-center gap-3">
            <Icon name="camera" size={26} />
            <Text>촬영 가이드</Text>
          </h2>
          <ul className="text-body m-0 list-disc rounded-[var(--radius-md)] bg-[#DFE8D4] py-3 pr-4 pl-9 break-words">
            <li className="whitespace-pre-line">{verification.photoGuide}</li>
          </ul>
        </section>
      </div>

      <div className="mt-auto pt-12">
        <p className="text-body mb-3 text-center">
          다른 목표를 입력하고 싶나요?{' '}
          <button
            type="button"
            onClick={onRetry}
            className="font-semibold underline underline-offset-4"
          >
            다시 입력하기
          </button>
        </p>
        <SuccessButton onClick={onAccept}>수락하고 도전 시작</SuccessButton>
      </div>
    </div>
  )
}
