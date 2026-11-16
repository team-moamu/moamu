import type { CollectionResponse } from './Collection'

export interface GalleryExhibitionResponse {
  challengeId: number
  isExhibited: boolean
}

export type GalleryResponse = Pick<CollectionResponse,
  'challengeId' | 'challengeTitle' | 'endDate' | 'progress' | 'artwork' | 'puzzle'
> & {
  goalText: string
  startDate: string
}
