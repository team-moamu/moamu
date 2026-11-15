interface ArtworkProgressBarProps {
  progressPercent: number
  className?: string
}

export function ArtworkProgressBar({ progressPercent, className = '' }: ArtworkProgressBarProps) {
  const percent = Number.isFinite(progressPercent) ? Math.min(100, Math.max(0, progressPercent)) : 0

  return (
    <div
      role="progressbar"
      aria-label="도전 성공률"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={percent}
      className={`relative h-[3px] w-full bg-[#EEE5D6] ${className}`}
    >
      <div className="h-full bg-[#4F7652]" style={{ width: `${percent}%` }} />
      {percent > 0 && (
        <span
          className="absolute top-1/2 h-[6px] w-[6px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-[#4F7652]"
          style={{ left: `clamp(3px, ${percent}%, calc(100% - 3px))` }}
        />
      )}
    </div>
  )
}
