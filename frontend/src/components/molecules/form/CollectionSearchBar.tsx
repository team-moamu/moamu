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
    <div className={`flex w-full items-center justify-center gap-2 ${className}`}>
      <SearchInput {...inputProps} />
      <MoreButton {...moreButtonProps} />
    </div>
  )
}
