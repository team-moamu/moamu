import type { ComponentProps } from 'react'
import { SearchInput } from '../../atoms/input/SearchInput'
import { MoreButton } from '../../atoms/button/MoreButton'

interface CollectionSearchBarProps {
  inputProps?: ComponentProps<typeof SearchInput>
  moreButtonProps?: ComponentProps<typeof MoreButton>
  className?: string
}

export function CollectionSearchBar({
  inputProps,
  moreButtonProps,
  className = '',
}: CollectionSearchBarProps) {
  return (
    <div className={`grid w-full grid-cols-[minmax(0,1fr)_32px] items-center gap-[2.28%] ${className}`}>
      <SearchInput
        {...inputProps}
        style={{ height: '32px', ...inputProps?.style }}
      />
      <MoreButton
        {...moreButtonProps}
        style={{ width: '32px', height: '32px', ...moreButtonProps?.style }}
      />
    </div>
  )
}
