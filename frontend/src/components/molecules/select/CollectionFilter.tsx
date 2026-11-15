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
    <div role="group" aria-label="작품 전시 상태 필터" className="mx-auto flex w-[90.6%] items-center justify-between">
      {filters.map((filter) => (
        <FilterButton
          key={filter.value}
          selected={value === filter.value}
          onClick={() => onChange(filter.value)}
          style={{ width: '27.2%', height: 'auto', aspectRatio: '173 / 52' }}
        >
          {filter.label}
        </FilterButton>
      ))}
    </div>
  )
}
