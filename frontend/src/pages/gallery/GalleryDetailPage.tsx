import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router'
import { getGalleries } from '../../api/GalleryApi'
import type { GalleryResponse } from '../../types/Gallery'
import { ArtworkPuzzle } from '../../components/organisms/puzzle/ArtworkPuzzle'
import { ArtworkProgressBar } from '../../components/atoms/progressBar/ArtworkProgressBar'
import { Icon } from '../../components/atoms/icon/Icon'
import profileMu from '../../assets/images/default_profile_mu.png'
import completeMu from '../../assets/images/complete_mu.png'

export function GalleryDetailPage() {
  const { challengeId } = useParams()
  const [artwork, setArtwork] = useState<GalleryResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()

    async function loadArtwork() {
      setLoading(true)
      setError(null)
      setArtwork(null)
      try {
        const id = Number(challengeId)
        if (!Number.isSafeInteger(id) || id <= 0) throw new Error('올바르지 않은 작품 주소예요.')
        // 전시 목록을 다시 조회해 직접 접속·새로고침에도 최신 작품을 표시한다.
        const galleries = await getGalleries('latest', controller.signal)
        if (!controller.signal.aborted) {
          setArtwork(galleries.find(item => item.challengeId === id) ?? null)
        }
      } catch (cause) {
        if (!controller.signal.aborted) {
          setError(cause instanceof Error ? cause.message : '작품을 불러오지 못했어요.')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }
    void loadArtwork()
    return () => controller.abort()
  }, [challengeId, retry])

  if (loading) return <p role="status" className="p-8 text-center text-sm">작품을 불러오는 중이에요.</p>
  if (error || !artwork) {
    return (
      <div className="flex flex-col items-center gap-4 px-6 py-12 text-center text-sm">
        <p role={error ? 'alert' : 'status'}>{error || '현재 전시 중인 작품을 찾을 수 없어요.'}</p>
        {error && <button type="button" onClick={() => setRetry(value => value + 1)}
          className="rounded-lg border border-[var(--color-border)] px-4 py-2">다시 시도</button>}
        <Link to="/museum" className="text-[var(--color-primary)] underline">미술관으로 돌아가기</Link>
      </div>
    )
  }

  const { puzzle, progress } = artwork
  return (
    <div className="flex flex-col gap-5 px-6 py-6">
      <section aria-label="작품 정보" className="flex items-center gap-3 rounded-xl border border-[#E8CFAC] bg-[#FFF8ED] p-3">
        <img src={profileMu} alt="" className="h-12 w-12 shrink-0 rounded-full border border-[#E8CFAC] object-cover" />
        <div className="min-w-0">
          <h2 className="break-words font-[family-name:var(--font-description)] text-[20px] font-bold">{artwork.challengeTitle}</h2>
          <p className="mt-1 font-[family-name:var(--font-ui)] text-[16px] font-normal text-[#89765F]">{artwork.startDate?.replaceAll('-', '.')} ~ {artwork.endDate.replaceAll('-', '.')}</p>
          <p className="mt-1 break-words font-[family-name:var(--font-ui)] text-[12px] font-normal">{artwork.goalText}</p>
        </div>
      </section>

      <figure>
        <ArtworkPuzzle imageUrl={artwork.artwork.imageUrl} title={artwork.artwork.artworkTitle}
          gridRows={puzzle.gridRows} gridCols={puzzle.gridCols}
          totalPieceCount={puzzle.totalPieceCount} revealedPieceIndices={puzzle.revealedPieceIndices} />
        <figcaption className="mt-4 text-center text-sm font-semibold">
          {puzzle.earnedPieceCount} / {puzzle.totalPieceCount} 조각 획득
        </figcaption>
        <div className="mt-3 px-3">
          <ArtworkProgressBar progressPercent={progress.progressPercent} />
          <p className="mt-1 text-right text-xs text-[#4F7652]">{progress.progressPercent}%</p>
        </div>
        <div className="mt-3 grid grid-cols-10 gap-1" aria-hidden="true">
          {Array.from({ length: puzzle.totalPieceCount }, (_, index) => (
            <Icon key={index} name="challenge" size={22}
              className={`w-full ${index < puzzle.earnedPieceCount ? 'text-[#4F7652]' : 'text-[#E7E6E1]'}`} />
          ))}
        </div>
      </figure>

      <aside className="flex items-center gap-3 overflow-hidden rounded-2xl border border-[#E8CFAC] bg-[#FFF8ED] p-4">
        <img src={completeMu} alt="" className="h-24 w-24 shrink-0 object-contain" />
        <div className="min-w-0 flex-1 text-right">
          <p className="text-[20px] text-[#89765F]">지금까지 <strong className="font-semibold text-[#4F7652]">정말 잘 하고 있어요!</strong></p>
          <p className="mt-2 text-[16px] leading-relaxed text-[#89765F]">하나씩 채워가는 퍼즐이<br />멋진 작품이 되고 있어요.</p>
        </div>
      </aside>
    </div>
  )
}
