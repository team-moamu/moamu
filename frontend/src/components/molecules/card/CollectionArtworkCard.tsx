import { ArtworkImage } from '../../atoms/image/ArtworkImage'
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
  onMoreClick?: () => void
}

export function CollectionArtworkCard({
  challengeTitle,
  artwork,
  progress,
  isExhibited,
  onMoreClick,
}: CollectionArtworkCardProps) {
  return (
    <article className="flex items-center gap-3 border-b border-[#F2E6D2] px-2 py-3">
      <ArtworkImage imageUrl={artwork.imageUrl} artworkTitle={artwork.artworkTitle} />
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
          className="flex h-8 w-6 cursor-pointer items-center justify-center rounded text-[#8A7762] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#4F7652]"
        >
          <Icon name="moreVertical" size={20} />
        </button>
      </div>
    </article>
  )
}
