export type CollectionSort = 'latest' | 'successRate' | 'name'

export interface CollectionResponse {
  challengeId: number
  challengeTitle: string
  challengeStatus: 'COMPLETED'
  endDate: string
  isExhibited: boolean
  progress: {
    durationDays: number
    successDays: number
    progressPercent: number
  }
  artwork: {
    artworkId: number
    artworkTitle: string
    imageUrl: string | null
  }
  puzzle: {
    gridRows: number
    gridCols: number
    revealedPieceIndices: number[]
    earnedPieceCount: number
    totalPieceCount: number
  }
}
