export type VerifiableGoalAnalysis = {
  status: 'VERIFIABLE'
  analysisId: string
  goal: string
  normalizedGoal: string
  conditions: string[]
  photoGuide: string
  verificationScope: string
}

export type GoalAnalysisResponse = VerifiableGoalAnalysis | {
  status: 'NOT_VERIFIABLE'
  alternatives: string[]
}

export function isVerifiableGoalAnalysis(value: unknown): value is VerifiableGoalAnalysis {
  if (!value || typeof value !== 'object') return false
  const result = value as Partial<VerifiableGoalAnalysis>
  return result.status === 'VERIFIABLE'
    && typeof result.analysisId === 'string' && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(result.analysisId)
    && [result.goal, result.normalizedGoal, result.photoGuide, result.verificationScope]
      .every((field) => typeof field === 'string' && field.trim() !== '')
    && Array.isArray(result.conditions) && result.conditions.length > 0
    && result.conditions.every((condition) => typeof condition === 'string' && condition.trim() !== '')
}
