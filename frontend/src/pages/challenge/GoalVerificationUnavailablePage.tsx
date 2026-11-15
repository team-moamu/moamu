import { useLocation, useNavigate } from 'react-router'
import { SuccessButton } from '../../components/atoms/button/successButton'
import { GoalVerificationUnavailable, type UnverifiableGoal } from '../../components/organisms/challenge/GoalVerificationUnavailable'

function isUnverifiableGoal(value: unknown): value is UnverifiableGoal {
  if (!value || typeof value !== 'object') return false
  const verification = value as Partial<UnverifiableGoal>
  return typeof verification.goal === 'string' && verification.goal.trim() !== ''
    && Array.isArray(verification.suggestions) && verification.suggestions.length > 0
    && verification.suggestions.every((goal) => typeof goal === 'string' && goal.trim() !== '')
}

export function GoalVerificationUnavailablePage() {
  const navigate = useNavigate()
  const location = useLocation()
  const verification: unknown = location.state?.verification

  const handleSelectGoal = (goal: string) => {
    navigate('/challenge/create', {
      state: {
        customGoal: goal,
        editCustomGoal: true,
        startDate: location.state?.startDate,
        durationDays: location.state?.durationDays,
      },
    })
  }

  return (
    <div className="flex flex-1 flex-col">
      {isUnverifiableGoal(verification) ? (
        <GoalVerificationUnavailable
          verification={verification}
          onSelectGoal={handleSelectGoal}
        />
      ) : (
        <div className="flex flex-1 flex-col gap-6 px-5 py-6">
          <p role="status" className="text-body">목표 판정 결과를 불러오지 못했어요. 목표를 다시 입력해 주세요.</p>
          <SuccessButton onClick={() => handleSelectGoal('')}>목표 입력하기</SuccessButton>
        </div>
      )}
    </div>
  )
}
