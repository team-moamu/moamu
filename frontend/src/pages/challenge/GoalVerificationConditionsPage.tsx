import { useLocation, useNavigate, useSearchParams } from 'react-router'
import { SuccessButton } from '../../components/atoms/button/SuccessButton'
import { GoalVerificationConditions, type GoalVerification } from '../../components/organisms/challenge/GoalVerificationConditions'
import { isVerifiableGoalAnalysis } from '../../types/GoalAnalysis'

const previewVerification: GoalVerification = {
  goal: '하루 한번 산책하기',
  conditions: [
    '야외 산책로나 길이 보여야 해요.',
    '걷는 모습이나 신발이 보여야 해요.',
  ],
  photoGuide: '산책 중인 길과 발끝이 함께 나오도록 촬영해 주세요.',
}

function isGoalVerification(value: unknown): value is GoalVerification {
  if (!value || typeof value !== 'object') return false
  const verification = value as Partial<GoalVerification>
  return typeof verification.goal === 'string' && verification.goal.trim() !== ''
    && Array.isArray(verification.conditions) && verification.conditions.length > 0
    && verification.conditions.every((condition) => typeof condition === 'string' && condition.trim() !== '')
    && typeof verification.photoGuide === 'string' && verification.photoGuide.trim() !== ''
}

export function GoalVerificationConditionsPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()
  const verification: unknown = searchParams.get('preview') === 'true'
    ? previewVerification
    : location.state?.verification

  const handleRetry = () => {
    navigate('/challenge/create', {
      replace: true,
      state: {
        startDate: location.state?.startDate,
        durationDays: location.state?.durationDays,
        customGoal: isGoalVerification(verification) ? verification.goal : '',
        editCustomGoal: true,
      },
    })
  }

  return (
    <div className="flex flex-1 flex-col">
      {isGoalVerification(verification) ? (
        <GoalVerificationConditions
          verification={verification}
          onRetry={handleRetry}
          onAccept={() => navigate('/challenge/create', {
            replace: true,
            state: {
              startDate: location.state?.startDate,
              durationDays: location.state?.durationDays,
              customGoal: verification.goal,
              acceptedVerification: isVerifiableGoalAnalysis(verification) ? verification : null,
            },
          })}
        />
      ) : (
        <div className="flex flex-1 flex-col gap-6 px-5 py-6">
          <p role="status" className="text-body">인증 조건을 확인하려면 목표를 입력하고 AI 판정을 받아 주세요.</p>
          <SuccessButton onClick={handleRetry}>목표 입력하기</SuccessButton>
        </div>
      )}
    </div>
  )
}
