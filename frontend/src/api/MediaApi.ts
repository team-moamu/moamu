import type { ApiResponse } from '../types/ApiResponse'

export async function uploadVerificationImage(
  image: File,
  signal?: AbortSignal,
): Promise<{ mediaId: number }> {
  const formData = new FormData()
  formData.append('image', image)

  const response = await fetch('/api/media', {
    method: 'POST',
    body: formData,
    signal,
  })
  const body: ApiResponse<{ mediaId: number }> | null =
    await response.json().catch(() => null)

  if (!response.ok || !body || !body.success) {
    throw new Error(body?.success === false
      ? body.error.message
      : '사진을 업로드하지 못했습니다. 다시 시도해 주세요.')
  }

  return body.data
}
