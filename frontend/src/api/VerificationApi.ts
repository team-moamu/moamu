import type { ApiResponse } from '../types/ApiResponse'
import type { VerificationSuccessResponse } from '../types/Challenge'

// 인증 성공 결과 조회. 실패해도 사진 인증을 다시 제출하지 않음
export async function getVerificationSuccess(
  verificationId: number,
  signal?: AbortSignal,
  accessToken?: string,
): Promise<VerificationSuccessResponse> {
  const response = await fetch(`/api/verification/${verificationId}/result`, {
    signal,
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined,
  })
  const body: ApiResponse<VerificationSuccessResponse> | null = await response.json().catch(() => null)

  if (!response.ok || !body || !body.success) {
    throw new Error(body?.success === false ? body.error.message : '인증 결과를 불러오지 못했어요.')
  }
  return body.data
}

