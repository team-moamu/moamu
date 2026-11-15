import type { GalleryResponse } from '../../../types/Gallery'
import { ArtworkImage } from '../../atoms/image/ArtworkImage'
import { ArtworkTitle } from '../../atoms/typography/ArtworkTitle'
import { SuccessDaysText } from '../../atoms/typography/SuccessDaysText'
import { ArtworkProgressBar } from '../../atoms/progressBar/ArtworkProgressBar'
import { ProgressPercent } from '../../atoms/typography/ProgressPercent'
import { ArtworkSelectButton } from '../../atoms/button/ArtworkSelectButton'

type GalleryArtworkCardProps = Pick<GalleryResponse, 'challengeTitle' | 'artwork' | 'progress'> & {
  selected?: boolean
  onToggleSelect?: () => void
}

export function GalleryArtworkCard({ challengeTitle, artwork, progress, selected = false, onToggleSelect }: GalleryArtworkCardProps) {
  return (
    <article className="relative min-w-0 overflow-hidden rounded-[10px] border border-[#4F7652] bg-white">
      {onToggleSelect && <ArtworkSelectButton selected={selected} label={`${challengeTitle} 선택`} onClick={onToggleSelect} />}
      <ArtworkImage imageUrl={artwork.imageUrl} artworkTitle={artwork.artworkTitle} variant="gallery" />
      <div className="p-2">
        <ArtworkTitle challengeTitle={challengeTitle} />
        <SuccessDaysText durationDays={progress.durationDays} successDays={progress.successDays} />
        <div className="mt-2">
          <ArtworkProgressBar progressPercent={progress.progressPercent} />
        </div>
        <div className="mt-1 flex justify-end">
          <ProgressPercent progressPercent={progress.progressPercent} />
        </div>
      </div>
    </article>
  )
}
