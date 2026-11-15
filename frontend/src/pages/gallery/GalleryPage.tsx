import { useEffect, useRef, useState } from 'react'
import { GalleryBanner } from '../../components/molecules/gallery/GalleryBanner'
import { GalleryToolbar } from '../../components/molecules/gallery/GalleryToolbar'
import type { CollectionSort } from '../../types/Collection'
import type { GalleryResponse } from '../../types/Gallery'
import { getGalleries, hideGalleryArtwork } from '../../api/GalleryApi'
import { GalleryArtworkCard } from '../../components/molecules/card/GalleryArtworkCard'
import { ConfirmDialog } from '../../components/molecules/modal/ConfirmDialog'
import { ErrorDialog } from '../../components/molecules/modal/ErrorDialog'

export function GalleryPage() {
  const [sort, setSort] = useState<CollectionSort>('latest')
  const [artworks, setArtworks] = useState<GalleryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)
  const [selectionMode, setSelectionMode] = useState(false)
  const [selectedIds, setSelectedIds] = useState<Set<number>>(new Set())
  const [hideDialogOpen, setHideDialogOpen] = useState(false)
  const [hiding, setHiding] = useState(false)
  const [hideError, setHideError] = useState(false)
  const hideInFlight = useRef(false)

  async function confirmHide() {
    if (hideInFlight.current || selectedIds.size === 0) return
    hideInFlight.current = true
    setHiding(true)
    setHideError(false)
    const ids = [...selectedIds]
    try {
      const results = await Promise.allSettled(ids.map(hideGalleryArtwork))
      const hidden = new Set<number>()
      const failed = new Set<number>()
      results.forEach((result, index) => {
        if (result.status === 'fulfilled') hidden.add(ids[index])
        else {
          failed.add(ids[index])
        }
      })
      setArtworks((current) => current.filter((artwork) => !hidden.has(artwork.challengeId)))
      setSelectedIds(failed)
      if (failed.size > 0) {
        setHideDialogOpen(false)
        setHideError(true)
      } else {
        setHideDialogOpen(false)
        setSelectionMode(false)
      }
    } finally {
      hideInFlight.current = false
      setHiding(false)
    }
  }

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
          selectionDisabled={hiding || loading || !!error || artworks.length === 0}
          hideDisabled={hiding || loading || !!error || selectedIds.size === 0}
          onHide={() => {
            setHideError(false)
            setHideDialogOpen(true)
          }}
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
      {hideDialogOpen && (
        <ConfirmDialog
          title="미술관에서 이미지를 숨길까요?"
          description={<>이미지는 삭제되지 않으며, 미술관에서만 표시되지 않습니다.<br />숨겨진 이미지는 <strong>보관함</strong>에서 확인할 수 있습니다.</>}
          confirmLabel={hiding ? '숨기는 중…' : '숨기기'}
          pending={hiding}
          onConfirm={confirmHide}
          onCancel={() => setHideDialogOpen(false)}
        />
      )}
      {hideError && (
        <ErrorDialog
          title="작품을 보관할 수 없습니다."
          description={<>현재 전시 중인 작품을 보관할 수 없습니다.<br />잠시 후 다시 시도해 주세요.</>}
          onClose={() => setHideError(false)}
        />
      )}
    </>
  )
}
