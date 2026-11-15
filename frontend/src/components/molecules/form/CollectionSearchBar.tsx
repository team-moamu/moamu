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
    <div className={`grid w-full grid-cols-[596fr_90fr] items-stretch gap-[2.28%] ${className}`}>
      <SearchInput
        {...inputProps}
        style={{ height: 'auto', aspectRatio: '596 / 64', ...inputProps?.style }}
      />
      <MoreButton
        {...moreButtonProps}
        style={{ width: '100%', height: '100%', ...moreButtonProps?.style }}
      />
    </div>
  )
}
