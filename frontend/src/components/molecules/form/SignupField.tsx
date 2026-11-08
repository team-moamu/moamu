import { TextInput } from '../../atoms/input/TextInput'
import type { TextInputProps } from '../../atoms/input/TextInput'
import { FormLabel } from '../../atoms/typography/FormLabel'
import { Description } from '../../atoms/typography/Description'

type SignupFieldProps = TextInputProps & {
  label: string
  hint?: string
  error?: string
  action?: React.ReactNode
}

// 라벨과 입력창을 묶고, 필요한 경우 안내 문구와 버튼을 함께 표시합니다.
export function SignupField({ label, hint, error, action, ...inputProps }: SignupFieldProps) {
  const errorId = error ? `${inputProps.id}-error` : undefined
  const hintId = hint ? `${inputProps.id}-hint` : undefined
  return (
    <div className="flex flex-col gap-2">
      <FormLabel htmlFor={inputProps.id}>{label}</FormLabel>
      <div className="flex items-center gap-2">
        <div className="min-w-0 flex-1">
          <TextInput {...inputProps} describedBy={errorId ?? hintId ?? inputProps.describedBy} />
        </div>
        {action}
      </div>
      {error && (
        <div id={errorId} aria-live="polite">
          <Description className="text-[var(--color-error)]">{error}</Description>
        </div>
      )}
      {!error && hint && (
        <div id={hintId}>
          <Description className="text-muted">{hint}</Description>
        </div>
      )}
    </div>
  )
}
