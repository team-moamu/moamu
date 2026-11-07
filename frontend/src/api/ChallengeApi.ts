import type {
  TodayChallengeResponse,
  ChallengeDetailResponse,
  ChallengeVerificationResponse,
} from '../types/Challenge'
import type { ApiResponse } from '../types/ApiResponse'

// 인증 제출 페이지 조회
export async function getChallengeVerification(
  challengeId: number,
  scheduleId: number,
  signal?: AbortSignal,
): Promise<ChallengeVerificationResponse | null> {
  const response = await fetch(
    `/api/verification?challengeId=${challengeId}&scheduleId=${scheduleId}`,
    { signal },
  )
  const body: ApiResponse<ChallengeVerificationResponse | null> | null =
    await response.json().catch(() => null)

  if (response.status === 404 && body?.success === false &&
      body.error.code === 'RESOURCE_NOT_FOUND') {
    return null
  }

  if (!response.ok || !body || !body.success) {
    throw new Error(
      body?.success === false
        ? body.error.message
        : '인증 정보를 불러오지 못했습니다.',
    )
  }

  return body.data
}

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
