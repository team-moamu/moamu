type ProgressBarProps = {
  value: number
  max: number
}

export function ProgressBar({ value, max }: ProgressBarProps) {
  const progress = max > 0
    ? Math.min(100, Math.max(0, (value / max) * 100))
    : 0

  return (
    <div className="h-2 w-full overflow-hidden rounded-full bg-[#E8E0D5]">
      <div
        className="h-full rounded-full bg-[#6B8E6B] transition-all duration-300"
        style={{ width: `${progress}%` }}
      />
    </div>
  )
}