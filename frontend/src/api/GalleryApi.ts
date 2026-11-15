import { authenticatedFetch } from './authenticatedFetch'
import type { ApiResponse } from '../types/ApiResponse'
import type { CollectionSort } from '../types/Collection'
import type { GalleryResponse } from '../types/Gallery'

export async function getGalleries(sort: CollectionSort = 'latest', signal?: AbortSignal): Promise<GalleryResponse[]> {
  const params = new URLSearchParams({ sort })
  const response = await authenticatedFetch(`/api/galleries?${params}`, { signal })
  const body: ApiResponse<GalleryResponse[]> | null = await response.json().catch(() => null)
  if (!response.ok || !body?.success) {
    throw new Error(body?.success === false ? body.error.message : '미술관 작품을 불러오지 못했어요.')
  }
  return body.data
}
