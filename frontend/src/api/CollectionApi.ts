import { authenticatedFetch } from './authenticatedFetch'
import type { ApiResponse } from '../types/ApiResponse'
import type { CollectionResponse, CollectionSort } from '../types/Collection'

interface CollectionQuery {
  exhibited?: boolean
  sort?: CollectionSort
  keyword?: string
}

export async function getCollections(
  { exhibited, sort = 'latest', keyword }: CollectionQuery = {},
  signal?: AbortSignal,
): Promise<CollectionResponse[]> {
  const params = new URLSearchParams({ sort })
  if (exhibited !== undefined) params.set('exhibited', String(exhibited))
  if (keyword?.trim()) params.set('keyword', keyword.trim())

  const response = await authenticatedFetch(`/api/collections?${params}`, { signal })
  const body: ApiResponse<CollectionResponse[]> | null = await response.json().catch(() => null)
  if (!response.ok || !body?.success) {
    throw new Error(body?.success === false ? body.error.message : '보관함을 불러오지 못했어요.')
  }
  return body.data
}
