import type { ApiResponse } from '../types/ApiResponse'
import type { VerificationSubmitResponse } from '../types/Challenge'
import { authenticatedFetch } from './authenticatedFetch'

// 기술 오류와 통신 오류를 구분하여 재제출 시 멱등성 키 처리
export class MediaVerificationError extends Error {
  readonly code: string | undefined

  constructor(message: string, code?: string) {
    super(message)
    this.name = 'MediaVerificationError'
    this.code = code
  }
}

// 사진 저장부터 AI 판정까지 하나의 요청으로 처리
export async function submitVerificationImage(
  image: File,
  scheduleId: number,
  idempotencyKey: string,
  signal?: AbortSignal,
  accessToken?: string,
): Promise<VerificationSubmitResponse> {
  // 인증 사진과 기존 회차 ID를 multipart/form-data로 전달
  const formData = new FormData()
  formData.append('image', image)
  formData.append('scheduleId', String(scheduleId))

  // 동일 제출의 재전송에는 같은 키 사용. 사용자 ID는 서버가 JWT에서 확인
  const headers: Record<string, string> = { 'Idempotency-Key': idempotencyKey }

  // multipart boundary는 브라우저가 설정하므로 Content-Type을 직접 지정하지 않음
  const response = await authenticatedFetch('/api/media', {
    method: 'POST',
    headers,
    body: formData,
    signal,
  }, accessToken)

  // 사진 저장 결과만이 아니라 AI 판정이 끝난 최종 응답을 반환
  const body: ApiResponse<VerificationSubmitResponse> | null =
    await response.json().catch(() => null)

  if (!response.ok || !body || !body.success) {
    throw new MediaVerificationError(
      body?.success === false ? body.error.message : '사진 인증에 실패했습니다. 다시 시도해 주세요.',
      body?.success === false ? body.error.code : undefined,
    )
  }

  return body.data
}
