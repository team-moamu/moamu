import type { TodayChallengeResponse } from '../types/Challenge'

export async function getTodayChallenges(): Promise<TodayChallengeResponse[]> {
  const response = await fetch('/api/challenges/today')

  if (!response.ok) {
    throw new Error('오늘의 도전 목록 조회 실패')
  }

  return response.json()
}