import { useEffect, useRef, useState } from 'react'
import { CollectionSortMenu } from '../../components/molecules/select/CollectionSortMenu'
import collectionMu from '../../assets/images/collection_mu.png'
import { getCollections } from '../../api/CollectionApi'
import type { CollectionResponse, CollectionSort } from '../../types/Collection'
import { CollectionArtworkCard } from '../../components/molecules/card/CollectionArtworkCard'
import { SectionTitle } from '../../components/atoms/typography/SectionTitle'
import { CollectionSearchBar } from '../../components/molecules/form/CollectionSearchBar'
import { CollectionFilter, type CollectionFilterValue } from '../../components/molecules/select/CollectionFilter'
import { CollectionEmptyState } from '../../components/molecules/feedback/CollectionEmptyState'

export function CollectionPage() {
  const [filter, setFilter] = useState<CollectionFilterValue>('all')
  const [keyword, setKeyword] = useState('')
  const [sort, setSort] = useState<CollectionSort>('latest')
  const [sortOpen, setSortOpen] = useState(false)
  const [collections, setCollections] = useState<CollectionResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [retry, setRetry] = useState(0)
  const [hasAnyArtwork, setHasAnyArtwork] = useState(true)
  const searchRef = useRef<HTMLDivElement>(null)

  function reloadCollection() {
    setLoading(true)
    setError(null)
    setSortOpen(false)
    setFilter('all')
    setKeyword('')
    setSort('latest')
    setRetry((value) => value + 1)
  }

  function closeSort() {
    setSortOpen(false)
    searchRef.current?.querySelector<HTMLButtonElement>('[aria-controls="collection-sort"]')?.focus()
  }

  useEffect(() => {
    if (!sortOpen) return
    function handlePointer(event: PointerEvent) {
      if (!searchRef.current?.contains(event.target as Node)) setSortOpen(false)
    }
    document.addEventListener('pointerdown', handlePointer)
    return () => document.removeEventListener('pointerdown', handlePointer)
  }, [sortOpen])

  useEffect(() => {
    const controller = new AbortController()
    const timer = window.setTimeout(async () => {
      setLoading(true)
      setError(null)
      try {
        const data = await getCollections({
          exhibited: filter === 'all' ? undefined : filter === 'exhibited',
          keyword,
          sort,
        }, controller.signal)
        // 검색 결과가 없는 것과 보관함 전체가 비어 있는 것을 구분한다.
        const hasFilter = filter !== 'all' || keyword.trim() !== ''
        const allItems = data.length === 0 && hasFilter
          ? await getCollections({}, controller.signal)
          : data
        if (!controller.signal.aborted) {
          setCollections(data)
          setHasAnyArtwork(allItems.length > 0)
        }
      } catch (cause) {
        if (!controller.signal.aborted) {
          setError(cause instanceof Error ? cause.message : '보관함을 불러오지 못했어요.')
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }, 300)
    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [filter, keyword, sort, retry])

  return (
    <>
      <div className="relative w-full">
        <img src={collectionMu} alt="" className="block w-full" />
      </div>
      <section className="px-6 py-5">
        <SectionTitle>진행된 작품</SectionTitle>
        {!loading && error ? (
          <CollectionEmptyState variant="error" onRetry={reloadCollection} />
        ) : !loading && !hasAnyArtwork ? (
          <CollectionEmptyState variant="empty" onRetry={reloadCollection} />
        ) : (
          <>
        <div className="mt-5">
          <CollectionFilter value={filter} onChange={setFilter} />
        </div>
        <div
          ref={searchRef}
          className="relative mt-[3.56%]"
          onKeyDown={(event) => {
            if (event.key === 'Escape' && sortOpen) closeSort()
          }}
          onBlur={(event) => {
            if (!event.currentTarget.contains(event.relatedTarget)) setSortOpen(false)
          }}
        >
        <CollectionSearchBar
          inputProps={{
            value: keyword,
            onChange: (event) => setKeyword(event.target.value),
          }}
          moreButtonProps={{
            onClick: () => setSortOpen((open) => !open),
            'aria-label': '작품 정렬 선택',
            'aria-expanded': sortOpen,
            'aria-controls': 'collection-sort',
          }}
        />
        {sortOpen && (
          <CollectionSortMenu value={sort} onChange={(value) => {
            setSort(value)
            closeSort()
          }} />
        )}
        </div>
        <div className="mt-1" aria-busy={loading}>
          {loading ? (
            <p role="status" className="py-6 text-center text-sm">작품을 불러오는 중이에요.</p>
          ) : error ? (
            <div role="alert" className="py-6 text-center text-sm">
              <p>{error}</p>
              <button type="button" onClick={() => setRetry((value) => value + 1)} className="mt-2 underline">다시 시도</button>
            </div>
          ) : (
            <>
              {collections.map((item) => (
                <CollectionArtworkCard key={item.challengeId} {...item} />
              ))}
              {collections.length === 0 && (
                <p className="py-6 text-center text-sm">
                  {keyword.trim() ? '검색 결과가 없어요.' : '해당하는 작품이 없어요.'}
                </p>
              )}
              <p role="status" className="mt-4 text-center text-sm">총 {collections.length}개</p>
            </>
          )}
        </div>
          </>
        )}
      </section>
    </>
  )
}
