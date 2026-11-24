import { Link } from 'react-router'
import { CanvasEasel } from '../../atoms/icon/CanvasEasel'
import { ArtworkPuzzle } from '../../organisms/puzzle/ArtworkPuzzle'
import type { GalleryResponse } from '../../../types/Gallery'

type GalleryEaselProps = {
  artwork: GalleryResponse
  left: number
  top: number
  width: number
  isNearby: boolean
}

export function GalleryEasel({ artwork, left, top, width, isNearby }: GalleryEaselProps) {
  const style = { left: `${left}%`, top: `${top}%`, width: `${width}%` }
  const content = (
    <>
      <CanvasEasel />
      <div className="gallery-easel-canvas">
        <ArtworkPuzzle imageUrl={artwork.artwork.imageUrl} title={artwork.artwork.artworkTitle}
          gridRows={artwork.puzzle.gridRows} gridCols={artwork.puzzle.gridCols}
          totalPieceCount={artwork.puzzle.totalPieceCount}
          revealedPieceIndices={artwork.puzzle.revealedPieceIndices} rounded={false} />
      </div>
    </>
  )

  // 가까운 작품만 링크로 만들어 클릭과 키보드 이동을 함께 허용한다.
  if (isNearby) {
    return (
      <Link to={`/museum/${artwork.challengeId}`} className="gallery-easel gallery-easel--nearby" style={style}
        title="클릭해서 작품 보기"
        aria-label={`${artwork.challengeTitle}, ${artwork.progress.durationDays}일 중 ${artwork.progress.successDays}일 성공. 작품 상세보기`}>
        {content}
      </Link>
    )
  }
  return (
    <div className="gallery-easel" style={style} title="뮤를 가까이 이동하면 작품을 볼 수 있어요.">
      {content}
    </div>
  )
}
