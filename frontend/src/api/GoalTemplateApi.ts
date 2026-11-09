import type { ApiResponse } from '../types/ApiResponse'
import { authenticatedFetch } from './authenticatedFetch'

export interface GoalTemplate {
  goalTemplateId: number
  goalName: string
  description: string
}

export async function getGoalTemplates(signal?: AbortSignal): Promise<GoalTemplate[]> {
  const response = await authenticatedFetch('/api/goalTemaplate', { signal })
  const body: ApiResponse<GoalTemplate[]> | null = await response.json().catch(() => null)
  if (!response.ok || !body?.success || !Array.isArray(body.data)) {
    throw new Error(body?.success === false ? body.error.message : '목표 목록을 불러오지 못했어요.')
  }
  return body.data
}
