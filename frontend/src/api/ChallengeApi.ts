import type {
  TodayChallengeResponse,
  ChallengeDetailResponse,
  ChallengeVerificationResponse,
  ChallengeCreateRequest,
  ChallengeCompletionResponse,
} from '../types/Challenge'
import type { ApiResponse } from '../types/ApiResponse'
import { authenticatedFetch } from './authenticatedFetch'

export class ChallengeCompletionError extends Error {
  readonly code: string | undefined

  constructor(message: string, code?: string) {
    super(message)
    this.name = 'ChallengeCompletionError'
    this.code = code
  }
}

// 완료 조회만 수행. 정산 POST나 인증 요청을 대신 실행하지 않음
export async function getChallengeCompletion(
  challengeId: number,
  signal?: AbortSignal,
  accessToken?: string,
): Promise<ChallengeCompletionResponse> {
  const response = await authenticatedFetch(`/api/challenges/${challengeId}/completion`, {
    signal,
  }, accessToken)
  const body: ApiResponse<ChallengeCompletionResponse> | null = await response.json().catch(() => null)
  if (!response.ok || !body || !body.success) {
    const code = body?.success === false ? body.error.code : undefined
    const message = code === 'CHALLENGE_NOT_COMPLETED' ? '아직 도전 결과가 확정되지 않았어요. 정산이 끝난 뒤 다시 확인해 주세요.'
      : body?.success === false ? body.error.message : '완료 결과를 불러오지 못했어요.'
    throw new ChallengeCompletionError(message, code)
  }
  if (!body.data || body.data.status !== 'COMPLETED' || body.data.challengeId !== challengeId) {
    throw new ChallengeCompletionError('완료 결과 정보가 올바르지 않아요.')
  }
  return body.data
}

// 인증 제출 페이지 조회
export async function getChallengeVerification(
  challengeId: number,
  scheduleId: number,
  signal?: AbortSignal,
  accessToken?: string,
): Promise<ChallengeVerificationResponse | null> {
  const response = await authenticatedFetch(
    `/api/verification?challengeId=${challengeId}&scheduleId=${scheduleId}`,
    { signal },
    accessToken,
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
export async function getTodayChallenges(
  day?: string,
  signal?: AbortSignal,
): Promise<TodayChallengeResponse[]> {
  const query = day ? `?day=${encodeURIComponent(day)}` : ''
  const response = await authenticatedFetch(`/api/challenges/today${query}`, { signal })

  if (!response.ok) {
    throw new Error('오늘의 도전 목록 조회 실패')
  }

  const result: ApiResponse<TodayChallengeResponse[]> =
    await response.json()

  if (!result.success || !result.data) {
    throw new Error(result.error?.message || '오늘의 도전 목록 조회 실패')
  }

  return result.data
}

// 도전 회차 상세 조회
export async function getChallengeDetail(
  challengeId: number,
  signal?: AbortSignal,
): Promise<ChallengeDetailResponse[]> {
  const response = await authenticatedFetch(
    `/api/schedule/detail?challengeId=${challengeId}`,
    { signal },
  )

  if (!response.ok) {
    throw new Error('도전 회차 상세 조회 실패')
  }

  const result: ApiResponse<ChallengeDetailResponse[]> =
    await response.json()

  if (!result.success || !result.data) {
    throw new Error(result.error?.message || '도전 회차 상세 조회 실패')
  }

  return result.data
}

// 도전 생성
export async function createChallenge(
  request: ChallengeCreateRequest
): Promise<void> {
  const response = await authenticatedFetch('/api/challenges', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)

    throw new Error(
      body?.error?.message || '도전을 생성하지 못했어요. 다시 시도해 주세요.'
    )
  }
}
