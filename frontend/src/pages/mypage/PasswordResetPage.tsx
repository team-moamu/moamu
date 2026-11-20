import { useState } from 'react'
import { SignupField } from '../../components/molecules/form/SignupField'
import { Button } from '../../components/atoms/button/Button'

export function PasswordResetPage() {
  const [email, setEmail] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [sendMessage, setSendMessage] = useState('')
  const isEmailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())

  return (
    <div className="space-y-6 px-6 pt-6 pb-28">
      <div>
        <h2 className="font-[family-name:var(--font-title)] text-[24px] leading-[36px]">
          가입한 이메일을 입력해 주세요.
        </h2>
        <p className="mt-2 text-xs leading-[18px]">
          인증번호를 보내드릴게요.
        </p>
      </div>

      <div className="flex flex-col gap-2">
        <SignupField
          id="reset-email"
          label="이메일"
          type="email"
          autoComplete="email"
          inputMode="email"
          placeholder="이메일을 입력해 주세요."
          value={email}
          onChange={(value) => {
            setEmail(value)
            setVerificationCode('')
            setSendMessage('')
          }}
        />

        <Button
          type="button"
          disabled={!isEmailValid}
          className="button button--secondary w-full font-normal"
          onClick={() => {
            setSendMessage('비밀번호 재설정 이메일 발송 기능은 준비 중이에요.')
          }}
        >
          인증번호 보내기
        </Button>

        {sendMessage && (
          <p role="status" className="text-caption text-muted">
            {sendMessage}
          </p>
        )}
      </div>

      <SignupField
        id="reset-verification-code"
        label="인증번호"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        placeholder="6자리 인증번호"
        value={verificationCode}
        onChange={(value) => {
          setVerificationCode(value.replace(/\D/g, ''))
        }}
        action={
          <button
            type="button"
            disabled
            className="button button--primary h-[52px] w-[88px] shrink-0 px-2"
          >
            인증하기
          </button>
        }
      />

      <div className="fixed inset-x-0 bottom-0 z-10 mx-auto w-full max-w-[430px] px-6 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
        <button
          type="button"
          disabled
          className="button button--primary button--full h-[52px]"
        >
          다음
        </button>
      </div>
    </div>
  )
}