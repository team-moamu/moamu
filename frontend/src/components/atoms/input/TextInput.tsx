import { Input } from './Input'

export type TextInputProps = {
  id: string
  value: string
  onChange: (value: string) => void
  type?: 'text' | 'email' | 'password'
  placeholder?: string
  autoComplete?: string
  inputMode?: 'text' | 'email' | 'numeric'
  maxLength?: number
  describedBy?: string
}

export function TextInput({
  id, value, onChange, type = 'text', placeholder, autoComplete,
  inputMode, maxLength, describedBy,
}: TextInputProps) {
  return (
    <Input
      id={id}
      name={id}
      type={type}
      value={value}
      onChange={(event) => onChange(event.target.value)}
      placeholder={placeholder}
      autoComplete={autoComplete}
      inputMode={inputMode}
      maxLength={maxLength}
      aria-describedby={describedBy}
      className="input min-h-13 rounded-[var(--radius-md)] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#426B45]"
    />
  )
}
