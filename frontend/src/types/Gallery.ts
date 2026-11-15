import type { CollectionResponse } from './Collection'

export type GalleryResponse = Pick<CollectionResponse,
  'challengeId' | 'challengeTitle' | 'endDate' | 'progress' | 'artwork' | 'puzzle'
>
