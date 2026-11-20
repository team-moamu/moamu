import { useState } from 'react'
import { SignupField } from '../../components/molecules/form/SignupField'

export function NewPasswordPage() {
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [message, setMessage] = useState('')

  const isPasswordValid =
    password.length >= 8 &&
    password.length <= 20 &&
    /^(?=.*[A-Za-z])(?=.*[0-9])[\x21-\x7E]+$/.test(password)

  const passwordsMatch = password === passwordConfirm
  const canSubmit = isPasswordValid && passwordsMatch

  return (
    <form
      className="space-y-8 px-6 pt-6 pb-28"
      onSubmit={(event) => {
        event.preventDefault()
        if (!canSubmit) return

        setMessage('비밀번호 변경 기능은 준비 중이에요.')
      }}
    >
      <h2 className="font-[family-name:var(--font-title)] text-[24px] leading-[36px]">
        새 비밀번호를 설정해 주세요.
      </h2>

      <SignupField
        id="new-password"
        label="새 비밀번호"
        type="password"
        autoComplete="new-password"
        placeholder="비밀번호를 입력해 주세요."
        value={password}
        onChange={(value) => {
          setPassword(value)
          setMessage('')
        }}
        hint="영문과 숫자를 포함해 공백 없이 8~20자로 설정해 주세요."
      />

      <SignupField
        id="new-password-confirm"
        label="새 비밀번호 확인"
        type="password"
        autoComplete="new-password"
        placeholder="비밀번호를 한번 더 입력해 주세요."
        value={passwordConfirm}
        onChange={(value) => {
          setPasswordConfirm(value)
          setMessage('')
        }}
        error={
          passwordConfirm && !passwordsMatch
            ? '비밀번호가 일치하지 않아요.'
            : undefined
        }
      />

      {message && (
        <p role="status" className="text-caption text-muted">
          {message}
        </p>
      )}

      <div className="fixed inset-x-0 bottom-0 z-10 mx-auto w-full max-w-[430px] px-6 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
        <button
          type="submit"
          disabled={!canSubmit}
          className="button button--primary button--full h-[52px]"
        >
          비밀번호 변경하기
        </button>
      </div>
    </form>
  )
}