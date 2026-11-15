import { useEffect, useState } from 'react'
import { GalleryBanner } from '../../components/molecules/gallery/GalleryBanner'
import { GalleryToolbar } from '../../components/molecules/gallery/GalleryToolbar'
import type { CollectionSort } from '../../types/Collection'
import type { GalleryResponse } from '../../types/Gallery'
import { getGalleries } from '../../api/GalleryApi'
import { GalleryArtworkCard } from '../../components/molecules/card/GalleryArtworkCard'

export function GalleryPage() {
  const [sort, setSort] = useState<CollectionSort>('latest')
  const [artworks, setArtworks] = useState<GalleryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)
  const [selectionMode, setSelectionMode] = useState(false)
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())

  function toggleSelectionMode() {
    setSelectionMode((value) => !value)
    setSelectedIds(new Set())
  }

  function toggleArtwork(challengeId: number) {
    setSelectedIds((current) => {
      const next = new Set(current)
      if (next.has(challengeId)) next.delete(challengeId)
      else next.add(challengeId)
      return next
    })
  }

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

  return (
    <>
      <GalleryBanner />
      <section className="px-6 py-5">
        <GalleryToolbar
          sort={sort}
          onSortChange={(value) => {
            if (value !== sort) {
              setLoading(true)
              setError(null)
            }
            setSort(value)
          }}
          selectionMode={selectionMode}
          onToggleSelection={toggleSelectionMode}
          selectionDisabled={loading || !!error || artworks.length === 0}
          hideDisabled={selectedIds.size === 0}
        />
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
              {artworks.map((artwork) => (
                <GalleryArtworkCard
                  key={artwork.challengeId}
                  {...artwork}
                  selected={selectedIds.has(artwork.challengeId)}
                  onToggleSelect={selectionMode ? () => toggleArtwork(artwork.challengeId) : undefined}
                />
              ))}
            </div>
          )}
        </div>
      </section>
    </>
  )
}
