interface ArtworkTitleProps {
  challengeTitle: string
  className?: string
}

export function ArtworkTitle({ challengeTitle, className = '' }: ArtworkTitleProps) {
  return (
    <h3 className={`m-0 truncate font-[family-name:var(--font-ui)] text-[14px] leading-5 font-semibold text-[#4A382A] ${className}`} title={challengeTitle}>
      {challengeTitle}
    </h3>
  )
}
