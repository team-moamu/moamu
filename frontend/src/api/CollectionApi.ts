import { authenticatedFetch } from './AuthenticatedFetch'
import type { ApiResponse } from '../types/ApiResponse'
import type { CollectionResponse, CollectionSort, CollectionSortDirection } from '../types/Collection'

interface CollectionQuery {
  exhibited?: boolean
  direction?: CollectionSortDirection
  sort?: CollectionSort
  keyword?: string
}

export async function exhibitCollection(challengeId: number): Promise<{ challengeId: number; isExhibited: boolean }> {
  const response = await authenticatedFetch(`/api/collections/${challengeId}/exhibition`, { method: 'PATCH' })
  const body: ApiResponse<{ challengeId: number; isExhibited: boolean }> | null = await response.json().catch(() => null)
  if (!response.ok || !body?.success) {
    throw new Error(body?.success === false ? body.error.message : '작품을 전시하지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
  if (body.data?.challengeId !== challengeId || body.data.isExhibited !== true) {
    throw new Error('작품의 전시 상태를 확인하지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
  return body.data
}

export async function getCollections(
  { exhibited, sort = 'endDate', direction = 'desc', keyword }: CollectionQuery = {},
  signal?: AbortSignal,
): Promise<CollectionResponse[]> {
  const params = new URLSearchParams({ sort, direction })
  if (exhibited !== undefined) params.set('exhibited', String(exhibited))
  if (keyword?.trim()) params.set('keyword', keyword.trim())

  const response = await authenticatedFetch(`/api/collections?${params}`, { signal })
  const body: ApiResponse<CollectionResponse[]> | null = await response.json().catch(() => null)
  if (!response.ok || !body?.success) {
    throw new Error(body?.success === false ? body.error.message : '보관함을 불러오지 못했어요.')
  }
  return body.data
}
