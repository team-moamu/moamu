import type { CollectionResponse } from '../../../types/Collection'
import { ArtworkPuzzle } from '../../organisms/puzzle/ArtworkPuzzle'
import { ArtworkTitle } from '../../atoms/typography/ArtworkTitle'
import { SuccessDaysText } from '../../atoms/typography/SuccessDaysText'
import { ArtworkProgressBar } from '../../atoms/progressBar/ArtworkProgressBar'
import { ProgressPercent } from '../../atoms/typography/ProgressPercent'
import { ExhibitionBadge } from '../../atoms/badge/ExhibitionBadge'
import { Icon } from '../../atoms/icon/Icon'

interface CollectionArtworkCardProps {
  challengeTitle: string
  artwork: {
    imageUrl: string | null
    artworkTitle: string
  }
  progress: {
    durationDays: number
    successDays: number
    progressPercent: number
  }
  isExhibited: boolean
  puzzle: CollectionResponse['puzzle']
  onMoreClick?: () => void
  moreExpanded?: boolean
  moreControls?: string
  moreDisabled?: boolean
}

export function CollectionArtworkCard({
  challengeTitle,
  artwork,
  progress,
  puzzle,
  isExhibited,
  onMoreClick,
  moreExpanded,
  moreControls,
  moreDisabled,
}: CollectionArtworkCardProps) {
  return (
    <article className="flex items-center gap-3 border-b border-[#F2E6D2] px-2 py-3">
      <div className="h-[60px] w-[60px] shrink-0 overflow-hidden rounded-[4px]">
        <ArtworkPuzzle
          imageUrl={artwork.imageUrl}
          title={artwork.artworkTitle}
          gridRows={puzzle.gridRows}
          gridCols={puzzle.gridCols}
          totalPieceCount={puzzle.totalPieceCount}
          revealedPieceIndices={puzzle.revealedPieceIndices}
          rounded={false}
        />
      </div>
      <div className="min-w-0 flex-1">
        <ArtworkTitle challengeTitle={challengeTitle} />
        <SuccessDaysText durationDays={progress.durationDays} successDays={progress.successDays} />
        <div className="mt-1 flex items-center gap-2">
          <ArtworkProgressBar progressPercent={progress.progressPercent} />
          <ProgressPercent progressPercent={progress.progressPercent} />
        </div>
      </div>
      <div className="flex shrink-0 items-center gap-1">
        <ExhibitionBadge isExhibited={isExhibited} />
        <button
          type="button"
          aria-label={`${challengeTitle} 더보기`}
          onClick={onMoreClick}
          aria-expanded={moreExpanded}
          aria-controls={moreControls}
          disabled={moreDisabled}
          className="flex h-8 w-6 cursor-pointer items-center justify-center rounded text-[#8A7762] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#4F7652]"
        >
          <Icon name="moreVertical" size={20} />
        </button>
      </div>
    </article>
  )
}
