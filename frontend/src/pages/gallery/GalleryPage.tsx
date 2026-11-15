import { useEffect, useRef, useState } from 'react'
import backgroundMu from '../../assets/images/background_mu.png'
import leaf from '../../assets/icons/leaf.svg'
import { GallerySectionHeader } from '../../components/molecules/gallery/GallerySectionHeader'
import { CollectionSortMenu } from '../../components/molecules/select/CollectionSortMenu'
import type { CollectionSort } from '../../types/Collection'
import type { GalleryResponse } from '../../types/Gallery'
import { getGalleries } from '../../api/GalleryApi'
import { GalleryArtworkCard } from '../../components/molecules/card/GalleryArtworkCard'

export function GalleryPage() {
  const [sort, setSort] = useState<CollectionSort>('latest')
  const [sortOpen, setSortOpen] = useState(false)
  const headerRef = useRef<HTMLDivElement>(null)
  const [artworks, setArtworks] = useState<GalleryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)

  useEffect(() => {
    const controller = new AbortController()
    async function loadArtworks() {
      try {
        const data = await getGalleries(sort, controller.signal)
        if (!controller.signal.aborted) setArtworks(data)
      } catch (cause) {
        if (!controller.signal.aborted) {
          setError(cause instanceof Error ? cause.message : '미술관 작품을 불러오지 못했어요.')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }
    void loadArtworks()
    return () => controller.abort()
  }, [sort, retry])

  function closeSort() {
    setSortOpen(false)
    headerRef.current?.querySelector<HTMLButtonElement>('[aria-controls="gallery-sort"]')?.focus()
  }

  useEffect(() => {
    if (!sortOpen) return
    function handlePointer(event: PointerEvent) {
      if (!headerRef.current?.contains(event.target as Node)) setSortOpen(false)
    }
    document.addEventListener('pointerdown', handlePointer)
    return () => document.removeEventListener('pointerdown', handlePointer)
  }, [sortOpen])

  return (
    <>
    <section aria-label="내 미술관" className="relative">
      <img src={backgroundMu} alt="" className="block w-full" />

      <div className="absolute top-[calc(25%+2px)] left-1/2 w-[76%] -translate-x-1/2 text-center text-[#4A382A]">
        <h2 className="m-0 font-[family-name:var(--font-description)] text-[18px] leading-[1.4] font-bold break-keep">
          나의 작은 미술관
        </h2>

        <p className="mt-[6px] mb-0 font-[family-name:var(--font-ui)] text-[12px] leading-[1.6] font-normal">
          하루하루의 기록이
          <br />
          <span className="relative inline-block font-semibold">
            <img
              src={leaf}
              alt=""
              className="pointer-events-none absolute bottom-[calc(100%-8px)] left-full h-4 w-4 -translate-x-1/2"
            />
            작품
          </span>으로 남아요.
        </p>
      </div>
    </section>
      <section className="px-6 py-5">
        <div
          ref={headerRef}
          className="relative"
          onKeyDown={(event) => {
            if (event.key === 'Escape' && sortOpen) closeSort()
          }}
          onBlur={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget)) setSortOpen(false)
          }}
        >
          <GallerySectionHeader moreButtonProps={{
            onClick: () => setSortOpen((open) => !open),
            'aria-label': '전시된 작품 정렬 선택',
            'aria-expanded': sortOpen,
            'aria-controls': 'gallery-sort',
          }} />
          {sortOpen && (
            <div className="absolute top-full right-[52px] z-20 mt-1 w-[116px]">
              <CollectionSortMenu id="gallery-sort" value={sort} onChange={(value) => {
                if (value !== sort) {
                  setLoading(true)
                  setError(null)
                }
                setSort(value)
                closeSort()
              }} />
            </div>
          )}
        </div>
        <div className="mt-4" aria-busy={loading}>
          {loading ? (
            <p role="status" className="py-6 text-center text-sm">작품을 불러오는 중이에요.</p>
          ) : error ? (
            <div role="alert" className="py-6 text-center text-sm">
              <p>{error}</p>
              <button type="button" onClick={() => {
                setLoading(true)
                setError(null)
                setRetry((value) => value + 1)
              }} className="mt-2 cursor-pointer underline">다시 시도</button>
            </div>
          ) : artworks.length === 0 ? (
            <p role="status" className="py-6 text-center text-sm">아직 전시된 작품이 없어요.</p>
          ) : (
            <div className="grid grid-cols-2 gap-x-4 gap-y-5">
              {artworks.map((artwork) => <GalleryArtworkCard key={artwork.challengeId} {...artwork} />)}
            </div>
          )}
        </div>
      </section>
    </>
  )
}
