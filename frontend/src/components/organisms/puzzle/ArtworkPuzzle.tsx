import { useState } from 'react'
import { puzzleLayout, puzzlePiecePath } from './puzzleGeometry'
import './ArtworkPuzzle.css'

interface ArtworkPuzzleProps {
  imageUrl: string | null
  title: string | null
  gridRows: number
  gridCols: number
  totalPieceCount: number
  revealedPieceIndices: number[]
  allPiecesEarned?: boolean
  highlightedPieceIndex?: number
}

// 인증 성공과 도전 완료 화면에서 동일한 획득 위치를 표시한다.
export function ArtworkPuzzle({
  imageUrl,
  title,
  gridRows,
  gridCols,
  totalPieceCount,
  revealedPieceIndices,
  allPiecesEarned = false,
  highlightedPieceIndex,
}: ArtworkPuzzleProps) {
  const [failedImageUrl, setFailedImageUrl] = useState<string | null>(null)
  const revealedPieces = new Set(revealedPieceIndices.filter(index => index >= 0 && index < totalPieceCount))
  const complete = allPiecesEarned || (totalPieceCount > 0 && revealedPieces.size === totalPieceCount)
  const imageAvailable = imageUrl && failedImageUrl !== imageUrl
  const highlightedLabel = highlightedPieceIndex === undefined ? '' : `, 이번 조각 ${highlightedPieceIndex + 1}번`
  const layout = puzzleLayout(totalPieceCount, gridRows, gridCols)
  const pieces = Array.from({ length: totalPieceCount }, (_, index) => ({
    index,
    path: puzzlePiecePath(index, layout),
    revealed: index < totalPieceCount && revealedPieces.has(index),
  }))
  const highlightedPiece = pieces.find(piece => piece.index === highlightedPieceIndex && piece.revealed)

  return (
    <div className="relative aspect-square overflow-hidden rounded-lg bg-[#e5dac5]"
      role="img"
      aria-label={`${title || '작품'} 퍼즐, ${totalPieceCount}개 중 ${complete ? totalPieceCount : revealedPieces.size}개 공개${highlightedLabel}`}>
      {imageAvailable ? (
        <img className="h-full w-full object-cover" src={imageUrl} alt="" onError={() => setFailedImageUrl(imageUrl)} />
      ) : (
        <p data-image-placeholder className="absolute inset-0 z-2 grid place-content-center p-3 text-center text-xs leading-6 text-[var(--color-text)] [text-shadow:0_0_4px_white]">작품 이미지를 불러올 수 없어요</p>
      )}
      {/* 미획득 조각은 반투명 검정으로 어둡게 표시하고, 전체 획득 시 원본 작품을 표시한다. */}
      {!complete && (
        <svg className="absolute inset-0 h-full w-full" viewBox="0 0 1000 1000" aria-hidden="true">
          {pieces.map(({ index, path, revealed }) => (
            <path key={index} d={path} data-piece-index={index} data-revealed={revealed}
              data-new={revealed && index === highlightedPieceIndex}
              fill={revealed ? 'none' : '#000000'} fillOpacity={revealed ? 1 : 0.75} stroke="#454545" strokeWidth="1"
              vectorEffect="non-scaling-stroke" />
          ))}
          {/* 마지막에 그려 인접한 미획득 조각이 강조 테두리를 덮지 않도록 한다. */}
          {highlightedPiece && (
            <path key={highlightedPiece.index} d={highlightedPiece.path} className="artwork-piece-highlight"
              fill="#ffc035" fillOpacity="0.14" stroke="#ffdd78" strokeWidth="3"
              vectorEffect="non-scaling-stroke" />
          )}
        </svg>
      )}
    </div>
  )
}
