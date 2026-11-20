import { useState } from 'react'
import { Link } from 'react-router'
import { SignupField } from '../../components/molecules/form/SignupField'

export function WithdrawalConfirmPage() {
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')

  return (
    <form
      className="px-6 pt-6 pb-48 text-[#493725]"
      onSubmit={(event) => {
        event.preventDefault()
        if (!password.trim()) return

        setMessage('회원탈퇴 기능은 준비 중이에요.')
      }}
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
          disabled={!password.trim()}
          className="button button--primary button--full h-[52px]"
        >
          회원 탈퇴
        </button>
      </div>
    </form>
  )
}