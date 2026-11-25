import type { ApiResponse } from '../types/ApiResponse'
import type { VerificationSubmitResponse } from '../types/Challenge'
import { authenticatedFetch } from './AuthenticatedFetch'

// 기술 오류와 통신 오류를 구분하여 재제출 시 멱등성 키 처리
export class MediaVerificationError extends Error {
  readonly code: string | undefined

  constructor(message: string, code?: string) {
    super(message)
    this.name = 'MediaVerificationError'
    this.code = code
  }
}

type VerificationProgress = Omit<VerificationSubmitResponse, 'status'> & {
  status: VerificationSubmitResponse['status'] | 'SUBMITTED' | 'ANALYZING' | 'TECH_ERROR'
}

// 다음 결과 조회까지 2초 대기하며, 취소 요청이 오면 대기를 중단
function waitForPoll(signal?: AbortSignal): Promise<void> {
  return new Promise((resolve, reject) => {
    if (signal?.aborted) { reject(new DOMException('Aborted', 'AbortError')); return }
    const abort = () => { clearTimeout(timer); reject(new DOMException('Aborted', 'AbortError')) }
    const timer = setTimeout(() => { signal?.removeEventListener('abort', abort); resolve() }, 2000)
    signal?.addEventListener('abort', abort, { once: true })
  })
}

// 사진 접수는 202로 끝나며 AI 처리는 서버 워커가 수행한다.
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

  // 접수 상태를 받은 뒤 결과 조회 API로 완료 여부를 확인
  const body: ApiResponse<VerificationProgress> | null =
    await response.json().catch(() => null)

  if (!response.ok || !body || !body.success) {
    throw new MediaVerificationError(
      body?.success === false ? body.error.message : '사진 인증에 실패했습니다. 다시 시도해 주세요.',
      body?.success === false ? body.error.code : undefined,
    )
  }

  const deadline = Date.now() + 5 * 60 * 1000
  let progress = body.data
  while (true) {
    if (progress.status === 'SUCCESS' || progress.status === 'FAILED' || progress.status === 'REVIEW_REQUIRED') {
      return { ...progress, status: progress.status }
    }
    if (progress.status === 'TECH_ERROR') {
      throw new MediaVerificationError('AI 처리 중 기술 오류가 발생했습니다. 인증 실패가 아니며 횟수는 복구됩니다. 다시 제출해 주세요.', 'AI_TECHNICAL_ERROR')
    }
    if (Date.now() >= deadline) {
      throw new MediaVerificationError('아직 판정 중입니다. 잠시 후 같은 사진으로 다시 확인해 주세요.')
    }
    await waitForPoll(signal)
    const statusResponse = await authenticatedFetch(`/api/media/${progress.verificationId}`, { signal }, accessToken)
    const statusBody: ApiResponse<VerificationProgress> | null = await statusResponse.json().catch(() => null)
    if (!statusResponse.ok || !statusBody?.success) {
      throw new MediaVerificationError(statusBody?.success === false ? statusBody.error.message : '판정 상태를 확인하지 못했습니다. 다시 확인해 주세요.')
    }
    progress = statusBody.data
  }
}
