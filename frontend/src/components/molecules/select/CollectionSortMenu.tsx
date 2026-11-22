import { useEffect, useRef } from 'react'
import { Icon, type IconName } from '../../atoms/icon/Icon'
import type { CollectionSortSelection } from '../../../types/Collection'

const options: { value: CollectionSortSelection; label: string; icon: IconName }[] = [
  { value: { sort: 'endDate', direction: 'desc' }, label: '최신순', icon: 'calendar' },
  { value: { sort: 'successRate', direction: 'desc' }, label: '성공률 높은순', icon: 'folder' },
  { value: { sort: 'name', direction: 'asc' }, label: '이름순', icon: 'alphabetical' },
]

interface CollectionSortMenuProps {
  id?: string
  value: CollectionSortSelection
  onChange: (value: CollectionSortSelection) => void
}

export function CollectionSortMenu({ value, onChange, id = 'collection-sort' }: CollectionSortMenuProps) {
  const selectedIndex = options.findIndex((option) => option.value.sort === value.sort)
  const orderedOptions = [
    ...options.slice(selectedIndex),
    ...options.slice(0, selectedIndex),
  ]
  const containerRef = useRef<HTMLDivElement>(null)
  useEffect(() => {
    containerRef.current?.querySelector<HTMLButtonElement>('[aria-pressed="true"]')?.focus()
  }, [])

  return (
    <div
      ref={containerRef}
      id={id}
      role="group"
      aria-label="작품 정렬"
      className="absolute top-0 right-0 z-20 w-[164px] rounded-[18px] bg-[#F2E6D2] px-4 py-3 text-[#4A382A]"
    >
      {orderedOptions.map((option, index) => (
        <button
          key={`${option.value.sort}-${option.value.direction}`}
          type="button"
          aria-pressed={option.value.sort === value.sort}
          aria-label={option.value.sort === value.sort
            ? `${option.label}, 현재 ${value.direction === 'asc' ? '오름차순' : '내림차순'}. 누르면 방향 전환`
            : option.label}
          onClick={() => onChange(option.value.sort === value.sort
            ? { sort: value.sort, direction: value.direction === 'asc' ? 'desc' : 'asc' }
            : option.value)}
          className={`flex w-full cursor-pointer items-center gap-2 whitespace-nowrap py-1 text-left font-[family-name:var(--font-ui)] text-[12px] leading-4 font-normal focus-visible:outline-2 focus-visible:outline-[#4F7652] ${index === 0 ? 'mb-1 border-b border-[#A89780] pb-2' : ''}`}
        >
          <Icon name={option.icon} size={14} />
          {option.label}
          {option.value.sort === value.sort && (
            <span className="ml-auto inline-flex" title={value.direction === 'asc' ? '오름차순' : '내림차순'}>
              <Icon name={value.direction === 'asc' ? 'sortAscending' : 'sortDescending'} size={14} />
            </span>
          )}
        </button>
      ))}
    </div>
  )
}
