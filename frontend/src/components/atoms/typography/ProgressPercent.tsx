interface ProgressPercentProps {
  progressPercent: number
  className?: string
}

export function ProgressPercent({ progressPercent, className = '' }: ProgressPercentProps) {
  const percent = Number.isFinite(progressPercent) ? Math.min(100, Math.max(0, progressPercent)) : 0

  return (
    <span className={`shrink-0 font-[family-name:var(--font-ui)] text-[16px] leading-5 font-semibold text-[#4F7652] tabular-nums ${className}`}>
      {percent}%
    </span>
  )
}
