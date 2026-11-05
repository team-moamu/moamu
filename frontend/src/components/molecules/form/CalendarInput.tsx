
interface CalendarInputProps {
  value: string
  onChange: (date: string) => void
  min?: string
}

export function CalendarInput({
  value,
  onChange,
  min,
}: CalendarInputProps) {
  return (
    <div className="relative w-full">
      <input
        type="date"
        value={value}
        min={min}
        onChange={(e) => onChange(e.target.value)}
        className="
          w-full rounded-lg border border-[#D8C8B5]
          bg-[#F4E7D4] px-4 py-3
          text-sm text-[#4B3A29]
          outline-none
          focus:border-[#6B8664]
        "
      />
    </div>
  )
}
