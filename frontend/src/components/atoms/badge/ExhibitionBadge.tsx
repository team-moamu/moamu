interface ExhibitionBadgeProps {
  isExhibited: boolean
  className?: string
}

export function ExhibitionBadge({ isExhibited, className = '' }: ExhibitionBadgeProps) {
  return (
    <span className={`inline-flex shrink-0 items-center justify-center whitespace-nowrap rounded-full px-2 py-1 font-[family-name:var(--font-ui)] text-[12px] leading-4 font-semibold ${
      isExhibited ? 'bg-[#DFEAD8] text-[#4F7652]' : 'bg-[#EDB6A2] text-[#A65F4B]'
    } ${className}`}>
      {isExhibited ? '전시 중' : '미전시'}
    </span>
  )
}
