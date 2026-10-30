
import { useState } from 'react'
import { GoalCardList } from '../../components/molecules/card/GoalCardList'
import { CalendarInput } from '../../components/molecules/form/CalendarInput'
import { DurationSelector } from '../../components/molecules/select/DurationSelector'
import { SuccessButton } from '../../components/atoms/button/successButton'
import type { IconName } from '../../components/atoms/icon/Icon'

// 임시 목표 데이터 (추후 API 연동)
const goals: {
  goalTemplateId: number
  title: string
  description: string
  icon: IconName
}[] = [
  { goalTemplateId: 1, title: '공부', description: '복습하기', icon: 'study' },
  { goalTemplateId: 2, title: '운동', description: '매일 계단 오르기', icon: 'exercise' },
  { goalTemplateId: 3, title: '물 마시기', description: '하루 2L', icon: 'water' },
  { goalTemplateId: 4, title: '알고리즘', description: '매일 1시간 학습', icon: 'algorithm' },
  { goalTemplateId: 5, title: '영양제', description: '매일 챙겨먹기', icon: 'supplement' },
  { goalTemplateId: 6, title: '명찰', description: '매일 명찰 챙기기', icon: 'nametag' },
]

export function ChallengeCreatePage() {
  // 목표 선택
  const [selectedGoalId, setSelectedGoalId] = useState<number | null>(null)

  // 시작 날짜
  const [startDate, setStartDate] = useState('')

  // 도전 기간
  const [durationDays, setDurationDays] = useState<number | null>(null)

  // 모든 항목 선택 여부
  const isDisabled =
    selectedGoalId === null ||
    startDate === '' ||
    durationDays === null

  // 도전 생성 버튼 클릭
  const handleCreateChallenge = () => {
    if (isDisabled) return

    // 추후 API 연동
    console.log({
      goalTemplateId: selectedGoalId,
      startDate,
      durationDays,
    })
  }

  return (
    <div className="flex flex-col gap-8 px-5 py-6">

      {/* 목표 선택 */}
      <section>
        <h2 className="mb-3 text-base font-semibold">
          어떤 도전을 만들어 볼까요?
        </h2>

        <GoalCardList
          goals={goals}
          value={selectedGoalId}
          onChange={setSelectedGoalId}
        />
      </section>

      {/* 시작 날짜 선택 */}
      <section>
        <h2 className="mb-3 text-base font-semibold">
          언제부터 시작할까요?
        </h2>

        <CalendarInput
          value={startDate}
          onChange={setStartDate}
        />
      </section>

      {/* 기간 선택 */}
      <section>
        <h2 className="mb-3 text-base font-semibold">
          얼마 동안 도전할까요?
        </h2>

        <DurationSelector
          value={durationDays}
          onChange={setDurationDays}
        />
      </section>

      {/* 도전 생성 버튼 */}
      <SuccessButton
        children="도전 생성하기"
        disabled={isDisabled}
        onClick={handleCreateChallenge}
      >
      </SuccessButton>

    </div>
  )
}
