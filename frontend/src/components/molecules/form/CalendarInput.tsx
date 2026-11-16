import { useId } from 'react'

interface CalendarInputProps {
  value: string
  onChange: (date: string) => void
  min?: string
  id?: string
  name?: string
}

export function CalendarInput({
  value,
  onChange,
  min,
  id,
  name,
}: CalendarInputProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId

  return (
    <div className="relative w-full">
      <input
        id={inputId}
        name={name ?? inputId}
        aria-label="날짜 선택"
        type="date"
        value={value}
        min={min}
        onChange={(e) => onChange(e.target.value)}
        className="
          w-full rounded-md border border-[#D8C8B5]
          bg-[#F4E7D4] px-4 py-3
          text-sm text-[#4B3A29]
          outline-none
          focus:border-[#6B8664]
        "
      />
    </div>
  )
}
