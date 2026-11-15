
import { useEffect, useRef, useState } from 'react'
import { useLocation, useNavigate } from 'react-router'
import { CustomChallengeCreateError, createCustomChallenge, createChallenge } from '../../api/ChallengeApi'
import { analyzeGoal } from '../../api/GoalAnalysisApi'
import { isVerifiableGoalAnalysis, type VerifiableGoalAnalysis } from '../../types/GoalAnalysis'
import { getGoalTemplates } from '../../api/GoalTemplateApi'
import { GoalCardList } from '../../components/molecules/card/GoalCardList'
import { CalendarInput } from '../../components/molecules/form/CalendarInput'
import { DurationSelector } from '../../components/molecules/select/DurationSelector'
import { SuccessButton } from '../../components/atoms/button/successButton'
import type { IconName } from '../../components/atoms/icon/Icon'
import { GoalCard } from '../../components/atoms/card/GoalCard'
import { CustomGoalModal } from '../../components/molecules/modal/CustomGoalModal'

type Goal = {
  goalTemplateId: number
  title: string
  description: string
  icon: IconName
}

function getGoalIcon(name: string): IconName {
  if (name.includes('명찰')) return 'nametag'
  if (name.includes('영양제')) return 'supplement'
  if (name.includes('알고리즘')) return 'algorithm'
  if (name.includes('물 마시기')) return 'water'
  if (name.includes('운동') || name.includes('계단')) return 'exercise'
  return 'study'
}

export function ChallengeCreatePage() {
  const navigate = useNavigate()
  const location = useLocation()
  const [goals, setGoals] = useState<Goal[]>([])
  const [isLoadingGoals, setIsLoadingGoals] = useState(true)
  // 목표 선택
  const [selectedGoalId, setSelectedGoalId] = useState<number | null>(null)
  const [customGoal] = useState<string>(
    typeof location.state?.customGoal === 'string' ? location.state.customGoal : '',
  )
  const [isCustomGoalModalOpen, setIsCustomGoalModalOpen] = useState(location.state?.editCustomGoal === true)

  const [analysis, setAnalysis] = useState<VerifiableGoalAnalysis | null>(
    isVerifiableGoalAnalysis(location.state?.acceptedVerification) ? location.state.acceptedVerification : null,
  )
  const [isAnalyzing, setIsAnalyzing] = useState(false)
  const [analysisError, setAnalysisError] = useState('')
  const analysisInFlight = useRef(false)
  const analysisController = useRef<AbortController | null>(null)
  useEffect(() => () => analysisController.current?.abort(), [])

  // 시작 날짜
  const [startDate, setStartDate] = useState<string>(location.state?.startDate ?? '')

  // 도전 기간
  const [durationDays, setDurationDays] = useState<number | null>(location.state?.durationDays ?? null)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [error, setError] = useState('')
  const submissionInFlight = useRef(false)

  useEffect(() => {
    const controller = new AbortController()
    getGoalTemplates(controller.signal)
      .then((templates) => {
        if (controller.signal.aborted) return
        setGoals(templates.map((template) => ({
          goalTemplateId: template.goalTemplateId,
          title: template.goalName,
          description: template.description,
          icon: getGoalIcon(template.goalName),
        })))
      })
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          setError(error instanceof Error ? error.message : '목표 목록을 불러오지 못했어요.')
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) setIsLoadingGoals(false)
      })
    return () => controller.abort()
  }, [])

  const handleAnalyzeGoal = async (goal: string) => {
    if (analysisInFlight.current) return
    analysisInFlight.current = true
    setIsAnalyzing(true)
    setAnalysisError('')
    const controller = new AbortController()
    analysisController.current = controller
    try {
      const result = await analyzeGoal(goal, controller.signal)
      if (controller.signal.aborted) return
      const draft = { customGoal: goal, startDate, durationDays }
      navigate(result.status === 'VERIFIABLE'
        ? '/challenge/create/conditions'
        : '/challenge/create/conditions/unverifiable', {
        state: {
          ...draft,
          verification: result.status === 'VERIFIABLE' ? result : { goal, suggestions: result.alternatives },
        },
      })
    } catch (error) {
      if (!controller.signal.aborted) setAnalysisError(error instanceof Error ? error.message : '목표를 분석하지 못했어요.')
    } finally {
      analysisInFlight.current = false
      if (!controller.signal.aborted) setIsAnalyzing(false)
    }
  }

  // 모든 항목 선택 여부
  const isDisabled =
    isSubmitting ||
    isAnalyzing ||
    (selectedGoalId === null && analysis === null) ||
    startDate === '' ||
    durationDays === null

  // 도전 생성 버튼 클릭
  const handleCreateChallenge = async () => {
    if (isDisabled || submissionInFlight.current || durationDays === null) return

    submissionInFlight.current = true
    setIsSubmitting(true)
    setError('')
    try {
      if (selectedGoalId !== null) {
        await createChallenge({ goalTemplateId: selectedGoalId, startDate, durationDays })
      } else if (analysis) {
        await createCustomChallenge({ analysisId: analysis.analysisId, startDate, durationDays })
      } else return
      // 도전 생성 성공하면 홈화면으로 이동(임시)
      navigate('/', { replace: true })
    } catch (error) {
      if (error instanceof CustomChallengeCreateError && ['ANALYSIS_NOT_FOUND', 'ANALYSIS_STALE'].includes(error.code ?? '')) {
        setAnalysis(null)
        setIsCustomGoalModalOpen(true)
        setAnalysisError(error.message)
      }
      setError(error instanceof Error ? error.message : '도전을 생성하지 못했어요. 다시 시도해 주세요.')
    } finally {
      submissionInFlight.current = false
      setIsSubmitting(false)
    }
  }

  return (
    <div className="flex flex-col gap-8 px-5 py-6">
      <fieldset disabled={isSubmitting || isAnalyzing} className="m-0 flex min-w-0 flex-col gap-8 border-0 p-0">

      {/* 목표 선택 */}
      <section>
        <h2 className="mb-3 text-base font-semibold">
          어떤 도전을 만들어 볼까요?
        </h2>

        {isLoadingGoals && <p role="status">목표 목록을 불러오는 중...</p>}
        {!isLoadingGoals && goals.length === 0 && !error && <p>선택할 수 있는 목표가 없어요.</p>}
        <GoalCardList
          goals={goals.filter((goal) => goal.icon !== 'nametag')}
          value={selectedGoalId}
          onChange={(id) => { setSelectedGoalId(id); setAnalysis(null) }}
        >
          <GoalCard
            title="나만의 도전"
            description={customGoal || '직접 생성하기'}
            icon="plus"
            selected={customGoal !== '' && selectedGoalId === null}
            onClick={() => { setAnalysisError(''); setIsCustomGoalModalOpen(true) }}
          />
        </GoalCardList>
      </section>

      {analysis && selectedGoalId === null && (
        <p role="status" className="text-sm">인증 조건을 수락했어요. 시작일과 기간을 선택해 도전을 생성해 주세요.</p>
      )}
      {/* 시작 날짜 선택 */}
      <section>
        <h2 className="mb-3 text-base font-semibold">
          언제부터 시작할까요?
        </h2>

        <CalendarInput
          id="challenge-start-date"
          name="startDate"
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

      </fieldset>

      {error && <p role="alert" className="m-0 text-sm text-[var(--color-error)]">{error}</p>}

      {/* 도전 생성 버튼 */}
      <SuccessButton
        disabled={isDisabled}
        onClick={handleCreateChallenge}
      >
        {isSubmitting ? '생성 중...' : '도전 생성하기'}
      </SuccessButton>

      {isCustomGoalModalOpen && (
        <CustomGoalModal
          value={customGoal}
          onClose={() => setIsCustomGoalModalOpen(false)}
          isAnalyzing={isAnalyzing}
          error={analysisError}
          onConfirm={handleAnalyzeGoal}
        />
      )}

    </div>
  )
}
