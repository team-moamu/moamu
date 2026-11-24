import { useState } from 'react'
import { SignupField } from '../../components/molecules/form/SignupField'
import { Button } from '../../components/atoms/button/Button'
import { useNavigate } from 'react-router'
import { sendPasswordResetCode, verifyPasswordResetCode } from '../../api/AuthApi'

export function PasswordResetPage() {
  const [email, setEmail] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [sendMessage, setSendMessage] = useState('')
  const isEmailValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email.trim())
  const [isSending, setIsSending] = useState(false)
  const navigate = useNavigate()
  const [sentEmail, setSentEmail] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [isVerifying, setIsVerifying] = useState(false)
  const [verifyError, setVerifyError] = useState('')

  const canVerify =
    sentEmail !== '' &&
    sentEmail === email.trim() &&
    /^[0-9]{6}$/.test(verificationCode) &&
    !isSending &&
    !isVerifying &&
    !resetToken

  async function handleSendCode() {
    if (!isEmailValid || isSending) return

    setIsSending(true)
    setSendMessage('')
    setVerificationCode('')
    setSentEmail('')
    setResetToken('')
    setVerifyError('')

    try {
      await sendPasswordResetCode(email.trim())
      setSentEmail(email.trim())
      setSendMessage('가입한 이메일이라면 인증번호를 보내드릴게요.')
    } catch (error) {
      setSendMessage(
        error instanceof Error
          ? error.message
          : '인증번호를 요청하지 못했어요. 다시 시도해 주세요.',
      )
    } finally {
      setIsSending(false)
    }
  }

  async function handleVerifyCode() {
    if (!canVerify) return

    setIsVerifying(true)
    setVerifyError('')

    try {
      const token = await verifyPasswordResetCode(sentEmail, verificationCode)
      setResetToken(token)
    } catch (error) {
      setVerifyError(
        error instanceof Error ? error.message : '인증번호 확인에 실패했어요.',
      )
    } finally {
      setIsVerifying(false)
    }
  }

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
          disabled={isSending || isVerifying}
          onChange={(value) => {
            setEmail(value)
            setVerificationCode('')
            setSendMessage('')
            setSentEmail('')
            setResetToken('')
            setVerifyError('')
          }}
        />

        <Button
          type="button"
          disabled={!isEmailValid || isSending || isVerifying}
          className="button button--secondary w-full font-normal"
          onClick={handleSendCode}
        >
          {isSending ? '요청 중…' : '인증번호 보내기'}
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
        disabled={isSending || isVerifying || Boolean(resetToken)}
        error={verifyError || undefined}
        hint={resetToken ? '이메일 인증을 완료했어요.' : undefined}
        onChange={(value) => {
          setVerificationCode(value.replace(/\D/g, ''))
        }}
        action={
          <button
            type="button"
            disabled={!canVerify}
            onClick={handleVerifyCode}
            className="button button--primary h-[52px] w-[88px] shrink-0 px-2"
          >
            인증하기
          </button>
        }
      />

      <div className="fixed inset-x-0 bottom-0 z-10 mx-auto w-full max-w-[430px] px-6 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
        <button
          type="button"
          disabled={!resetToken}
          onClick={() => navigate('/password-reset/new', { state: { resetToken } })}
          className="button button--primary button--full h-[52px]"
        >
          다음
        </button>
      </div>
    </div>
  )
}