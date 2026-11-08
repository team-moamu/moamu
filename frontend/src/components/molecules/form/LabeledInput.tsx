import type { InputHTMLAttributes } from 'react'
import { Input } from '../../atoms/input/Input'

type LabeledInputProps = InputHTMLAttributes<HTMLInputElement> & {
  id: string
  label: string
  error?: string
}

export function LabeledInput({
  id,
  label,
  error,
  className = '',
  'aria-describedby': describedBy,
  'aria-invalid': invalid,
  ...props
}: LabeledInputProps) {
  const errorId = `${id}-error`

  const descriptionIds = [
    describedBy,
    error ? errorId : undefined,
  ]
    .filter(Boolean)
    .join(' ') || undefined

  return (
    <div className="field">
      <label htmlFor={id} className="text-body font-bold">
        {label}
      </label>

      <Input
        {...props}
        id={id}
        className={`input ${className}`}
        aria-invalid={error ? true : invalid}
        aria-describedby={descriptionIds}
      />

      {error && (
        <p id={errorId} className="field__error">
          {error}
        </p>
      )}
    </div>
  )
}