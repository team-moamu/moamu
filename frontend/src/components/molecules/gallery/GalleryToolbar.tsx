import { useEffect, useId, useRef, useState } from 'react'
import type { CollectionSort } from '../../../types/Collection'
import { GallerySectionHeader } from './GallerySectionHeader'
import { CollectionSortMenu } from '../select/CollectionSortMenu'

interface GalleryToolbarProps {
  sort: CollectionSort
  onSortChange: (sort: CollectionSort) => void
  selectionMode: boolean
  onToggleSelection: () => void
  selectionDisabled: boolean
  hideDisabled: boolean
  onHide?: () => void
}

export function GalleryToolbar({ sort, onSortChange, selectionMode, onToggleSelection, selectionDisabled, hideDisabled, onHide }: GalleryToolbarProps) {
  const [sortOpen, setSortOpen] = useState(false)
  const containerRef = useRef<HTMLDivElement>(null)
  const menuId = useId()

  function closeSort() {
    setSortOpen(false)
    containerRef.current?.querySelector<HTMLButtonElement>('[aria-controls]')?.focus()
  }

  useEffect(() => {
    if (!sortOpen) return
    function handlePointer(event: PointerEvent) {
      if (!containerRef.current?.contains(event.target as Node)) setSortOpen(false)
    }
    document.addEventListener('pointerdown', handlePointer)
    return () => document.removeEventListener('pointerdown', handlePointer)
  }, [sortOpen])

  return (
    <div ref={containerRef} className="relative"
      onKeyDown={(event) => { if (event.key === 'Escape' && sortOpen) closeSort() }}
      onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) setSortOpen(false) }}
    >
      <GallerySectionHeader
        selectionMode={selectionMode}
        selectButtonProps={{
          onClick: () => { setSortOpen(false); onToggleSelection() },
          'aria-pressed': selectionMode,
          'aria-label': selectionMode ? '작품 선택 모드 종료' : '작품 선택 모드 시작',
          disabled: selectionDisabled,
        }}
        hideButtonProps={{ disabled: hideDisabled, onClick: onHide }}
        moreButtonProps={{
          onClick: () => setSortOpen((open) => !open),
          'aria-label': '전시된 작품 정렬 선택',
          'aria-expanded': sortOpen,
          'aria-controls': menuId,
        }}
      />
      {sortOpen && !selectionMode && (
        <div className="absolute top-full right-[52px] z-20 mt-1 w-[116px]">
          <CollectionSortMenu id={menuId} value={sort} onChange={(value) => {
            onSortChange(value)
            closeSort()
          }} />
        </div>
      )}
    </div>
  )
}
