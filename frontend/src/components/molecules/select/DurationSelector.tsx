
import { DurationButton } from '../../atoms/button/durationButton'

interface DurationSelectorProps {
  value: number | null
  onChange: (days: number) => void
}

export function DurationSelector({
  value,
  onChange,
}: DurationSelectorProps) {
  return (
    <div className="flex gap-2">
      {[7, 14, 30].map((days) => (
        <DurationButton
          key={days}
          days={days}
          selected={value === days}
          onClick={() => onChange(days)}
        />
      ))}
    </div>
  )
}
