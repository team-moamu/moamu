export interface ChallengeCreateRequest {
  goalTemplateId: number
  startDate: string
  durationDays: number
}

export async function createChallenge(request: ChallengeCreateRequest): Promise<void> {
  const response = await fetch('/api/challenges', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(request),
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    throw new Error(
      body?.error?.message || '도전을 생성하지 못했어요. 다시 시도해 주세요.',
    )
  }
}
