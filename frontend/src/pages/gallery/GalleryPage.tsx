import { useEffect, useRef, useState } from 'react'
import backgroundMu from '../../assets/images/background_mu.png'
import leaf from '../../assets/icons/leaf.svg'
import { GallerySectionHeader } from '../../components/molecules/gallery/GallerySectionHeader'
import { CollectionSortMenu } from '../../components/molecules/select/CollectionSortMenu'
import type { CollectionSort } from '../../types/Collection'

export function GalleryPage() {
  const [sort, setSort] = useState<CollectionSort>('latest')
  const [sortOpen, setSortOpen] = useState(false)
  const headerRef = useRef<HTMLDivElement>(null)

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
                setSort(value)
                closeSort()
              }} />
            </div>
          )}
        </div>
      </section>
    </>
  )
}
