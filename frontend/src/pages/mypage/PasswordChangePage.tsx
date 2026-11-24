import { useState } from 'react'
import type { FormEvent } from 'react'
import { Navigate } from 'react-router'
import { changePassword } from '../../api/AuthApi'
import { getAccessToken } from '../../auth/AuthStorage'
import { SignupField } from '../../components/molecules/form/SignupField'

export function PasswordChangePage() {
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')
  const [successMessage, setSuccessMessage] = useState('')

  const isNewPasswordValid =
    newPassword.length >= 8 &&
    newPassword.length <= 20 &&
    /^(?=.*[A-Za-z])(?=.*[0-9])[\x21-\x7E]+$/.test(newPassword)

  const passwordsMatch = newPassword === passwordConfirm
  const canSubmit =
    currentPassword.trim().length > 0 &&
    isNewPasswordValid &&
    passwordsMatch &&
    !isSubmitting

  function clearMessages() {
    setErrorMessage('')
    setSuccessMessage('')
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canSubmit) return

    clearMessages()
    setIsSubmitting(true)

    try {
      await changePassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setPasswordConfirm('')
      setSuccessMessage('비밀번호를 변경했어요.')
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : '비밀번호 변경 결과를 확인하지 못했어요.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!getAccessToken()) {
    return <Navigate to="/login" replace />
  }

  return (
    <form className="space-y-6 px-6 py-6" onSubmit={handleSubmit}>
      <SignupField
        id="current-password"
        label="현재 비밀번호"
        type="password"
        autoComplete="current-password"
        value={currentPassword}
        disabled={isSubmitting}
        onChange={(value) => {
          setCurrentPassword(value)
          clearMessages()
        }}
      />

      <SignupField
        id="new-password"
        label="새 비밀번호"
        type="password"
        autoComplete="new-password"
        value={newPassword}
        disabled={isSubmitting}
        hint="영문과 숫자를 포함해 공백 없이 8~20자로 설정해 주세요."
        onChange={(value) => {
          setNewPassword(value)
          clearMessages()
        }}
      />

      <SignupField
        id="new-password-confirm"
        label="새 비밀번호 확인"
        type="password"
        autoComplete="new-password"
        value={passwordConfirm}
        disabled={isSubmitting}
        error={
          passwordConfirm && !passwordsMatch
            ? '새 비밀번호가 일치하지 않아요.'
            : undefined
        }
        onChange={(value) => {
          setPasswordConfirm(value)
          clearMessages()
        }}
      />

      {errorMessage && (
        <p role="alert" className="text-sm text-[var(--color-error)]">
          {errorMessage}
        </p>
      )}

      {successMessage && (
        <p role="status" className="text-sm">
          {successMessage}
        </p>
      )}

      <button
        type="submit"
        disabled={!canSubmit}
        className="button button--primary button--full h-[52px]"
      >
        {isSubmitting ? '변경 중…' : '비밀번호 변경하기'}
      </button>
    </form>
  )
}