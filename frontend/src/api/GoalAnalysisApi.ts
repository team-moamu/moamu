import type { ApiResponse } from '../types/ApiResponse'
import { isVerifiableGoalAnalysis, type GoalAnalysisResponse } from '../types/GoalAnalysis'
import { authenticatedFetch } from './AuthenticatedFetch'

export async function analyzeGoal(goal: string, signal?: AbortSignal): Promise<GoalAnalysisResponse> {
  const response = await authenticatedFetch('/api/goals/analyze', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ goal }),
    signal,
  })
  const body: ApiResponse<GoalAnalysisResponse> | null = await response.json().catch(() => null)
  if (!response.ok || !body || !body.success) {
    throw new Error(body?.success === false ? body.error.message : '목표를 분석하지 못했어요. 다시 시도해 주세요.')
  }
  const result = body.data
  if (isVerifiableGoalAnalysis(result)) return result
  if (result?.status === 'NOT_VERIFIABLE' && Array.isArray(result.alternatives)
    && result.alternatives.length === 3
    && result.alternatives.every((goal) => typeof goal === 'string' && goal.trim() !== '')) return result
  throw new Error('목표 분석 응답을 확인하지 못했어요. 다시 시도해 주세요.')
}
