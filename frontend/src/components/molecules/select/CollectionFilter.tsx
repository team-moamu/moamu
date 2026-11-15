import { FilterButton } from '../../atoms/button/FilterButton'

export type CollectionFilterValue = 'all' | 'exhibited' | 'unexhibited'

const filters: { value: CollectionFilterValue; label: string }[] = [
  { value: 'all', label: '전체 작품' },
  { value: 'exhibited', label: '전시 중' },
  { value: 'unexhibited', label: '미전시' },
]

type CollectionFilterProps = {
  value: CollectionFilterValue
  onChange: (value: CollectionFilterValue) => void
}

export function CollectionFilter({ value, onChange }: CollectionFilterProps) {
  return (
    <div role="group" aria-label="작품 전시 상태 필터" className="flex items-center justify-center gap-[29px]">
      {filters.map((filter) => (
        <FilterButton
          key={filter.value}
          selected={value === filter.value}
          onClick={() => onChange(filter.value)}
          className="w-[102px] min-w-0 px-2"
        >
          {filter.label}
        </FilterButton>
      ))}
    </div>
  )
}
