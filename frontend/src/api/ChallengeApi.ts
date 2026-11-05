import type {
  TodayChallengeResponse,
  ChallengeDetailResponse,
} from '../types/Challenge'

// 오늘의 도전 목록 조회
export async function getTodayChallenges(): Promise<TodayChallengeResponse[]> {
  const response = await fetch('/api/challenges/today')

  if (!response.ok) {
    throw new Error('오늘의 도전 목록 조회 실패')
  }

  return response.json()
}

// 도전 회차 상세 조회
export async function getChallengeDetail(
  challengeId: number
): Promise<ChallengeDetailResponse[]> {
  const response = await fetch(
    `/api/schedule/detail?challengeId=${challengeId}`
  )

  if (!response.ok) {
    throw new Error('도전 회차 상세 조회 실패')
  }

  return response.json()
}