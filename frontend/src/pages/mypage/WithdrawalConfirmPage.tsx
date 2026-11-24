import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router'
import { withdrawAccount } from '../../api/AuthApi'
import { clearAccessToken, getAccessToken } from '../../auth/AuthStorage'
import { SignupField } from '../../components/molecules/form/SignupField'

export function WithdrawalConfirmPage() {
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')
  const navigate = useNavigate()
  const [isSubmitting, setIsSubmitting] = useState(false)
  const canSubmit = password.trim().length > 0 && !isSubmitting

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!canSubmit) return

    setMessage('')
    setIsSubmitting(true)

    try {
      await withdrawAccount(password)
      clearAccessToken()
      navigate('/mypage/withdrawal/complete', { replace: true })
    } catch (error) {
      setMessage(
        error instanceof Error
          ? error.message
          : '회원 탈퇴 중 오류가 발생했어요.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  if (!getAccessToken()) {
    return <Navigate to="/login" replace />
  }

  return (
    <form
      className="px-6 pt-6 pb-48 text-[#493725]"
      onSubmit={handleSubmit}
    >
      <h2 className="font-[family-name:var(--font-title)] text-[24px] leading-[36px]">
        탈퇴를 진행할까요?
      </h2>
      <p className="mt-2 text-[12px] leading-[18px]">
        비밀번호를 확인 후 탈퇴해요.
      </p>

      <div className="mt-6">
        <SignupField
          id="withdrawal-password"
          label="현재 비밀번호"
          type="password"
          autoComplete="current-password"
          placeholder="비밀번호를 입력해 주세요."
          value={password}
          disabled={isSubmitting}
          onChange={(value) => {
            setPassword(value)
            setMessage('')
          }}
        />
      </div>

      {message && (
        <p role="status" className="mt-3 text-sm text-muted">
          {message}
        </p>
      )}

      <div className="fixed inset-x-0 bottom-0 z-10 mx-auto flex w-full max-w-[430px] flex-col gap-4 bg-white px-6 pt-4 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
        <Link
          to="/mypage/account-security"
          className="button button--primary button--full h-[52px]"
        >
          계속 이용하기
        </Link>

        <button
          type="submit"
          disabled={!canSubmit}
          className="button button--primary button--full h-[52px]"
        >
          {isSubmitting ? '탈퇴 처리 중…' : '회원 탈퇴'}
        </button>
      </div>
    </form>
  )
}