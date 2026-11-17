import { authenticatedFetch } from './AuthenticatedFetch'
import type { ApiResponse } from '../types/ApiResponse'
import type { CollectionSort } from '../types/Collection'
import type { GalleryResponse, GalleryExhibitionResponse } from '../types/Gallery'

export async function hideGalleryArtwork(challengeId: number): Promise<GalleryExhibitionResponse> {
  const response = await authenticatedFetch(`/api/galleries/${challengeId}/exhibition`, { method: 'PATCH' })
  const body: ApiResponse<GalleryExhibitionResponse> | null = await response.json().catch(() => null)
  if (!response.ok || !body?.success) {
    throw new Error(body?.success === false ? body.error.message : '작품을 숨기지 못했어요. 다시 시도해 주세요.')
  }
  if (body.data?.challengeId !== challengeId || body.data.isExhibited !== false) {
    throw new Error('작품의 전시 상태를 확인하지 못했어요. 다시 시도해 주세요.')
  }
  return body.data
}

export async function getGalleries(sort: CollectionSort = 'latest', signal?: AbortSignal): Promise<GalleryResponse[]> {
  const params = new URLSearchParams({ sort })
  const response = await authenticatedFetch(`/api/galleries?${params}`, { signal })
  const body: ApiResponse<GalleryResponse[]> | null = await response.json().catch(() => null)
  if (!response.ok || !body?.success) {
    throw new Error(body?.success === false ? body.error.message : '미술관 작품을 불러오지 못했어요.')
  }
  return body.data
}
