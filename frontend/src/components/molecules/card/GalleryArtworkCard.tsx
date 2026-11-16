import type { GalleryResponse } from '../../../types/Gallery'
import { Link } from 'react-router'
import { ArtworkImage } from '../../atoms/image/ArtworkImage'
import { ArtworkTitle } from '../../atoms/typography/ArtworkTitle'
import { SuccessDaysText } from '../../atoms/typography/SuccessDaysText'
import { ArtworkProgressBar } from '../../atoms/progressBar/ArtworkProgressBar'
import { ProgressPercent } from '../../atoms/typography/ProgressPercent'
import { ArtworkSelectButton } from '../../atoms/button/ArtworkSelectButton'

type GalleryArtworkCardProps = Pick<GalleryResponse, 'challengeTitle' | 'artwork' | 'progress'> & {
  selected?: boolean
  detailUrl?: string
  onToggleSelect?: () => void
}

export function GalleryArtworkCard({ challengeTitle, artwork, progress, selected = false, onToggleSelect, detailUrl }: GalleryArtworkCardProps) {
  return (
    <article className="relative min-w-0 overflow-hidden rounded-[10px] border border-[#4F7652] bg-white">
      {detailUrl && !onToggleSelect && (
        <Link to={detailUrl} aria-label={`${challengeTitle} 작품 상세보기`}
          className="absolute inset-0 z-10 rounded-[10px] transition-colors hover:bg-black/5 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-[var(--color-primary)]" />
      )}
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
