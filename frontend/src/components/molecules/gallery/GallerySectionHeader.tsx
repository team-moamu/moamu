import type { ComponentProps } from 'react'
import { MoreButton } from '../../atoms/button/MoreButton'
import { SelectButton } from '../../atoms/button/SelectButton'
import { SectionTitle } from '../../atoms/typography/SectionTitle'

type GallerySectionHeaderProps = {
  selectionMode?: boolean
  hideButtonProps?: ComponentProps<typeof SelectButton>
  moreButtonProps?: ComponentProps<typeof MoreButton>
  selectButtonProps?: ComponentProps<typeof SelectButton>
}

export function GallerySectionHeader({ moreButtonProps, selectButtonProps, selectionMode = false, hideButtonProps }: GallerySectionHeaderProps) {
  return (
    <div className="flex items-center justify-between gap-3">
      <SectionTitle>전시된 작품</SectionTitle>
      <div className="flex shrink-0 items-center gap-2">
        {selectionMode ? (
          <SelectButton {...hideButtonProps}>미술관에서 숨기기</SelectButton>
        ) : (
          <MoreButton aria-label="전시된 작품 더보기" {...moreButtonProps} />
        )}
        <SelectButton {...selectButtonProps} />
      </div>
    </div>
  )
}
