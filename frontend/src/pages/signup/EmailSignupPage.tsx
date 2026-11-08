import { useState } from 'react'
import { Heading } from '../../components/atoms/typography/Heading'
import { Description } from '../../components/atoms/typography/Description'
import { Button } from '../../components/atoms/button/Button'
import { SignupField } from '../../components/molecules/form/SignupField'

export function EmailSignupPage() {
  const [email, setEmail] = useState('')
  const [emailError, setEmailError] = useState('')
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [passwordError, setPasswordError] = useState('')
  const canContinue = password.length >= 8 && passwordConfirm.length >= 8

  function handleNext() {
    if (!canContinue) return
    setEmailError(email.trim() ? '' : '이메일을 입력해 주세요.')
    setPasswordError(password !== passwordConfirm ? '비밀번호가 일치하지 않습니다.' : '')
    // 다음 단계 이동은 이메일 인증 API 연동 후 연결합니다.
  }

  // 실제 이메일 인증 API가 연결되기 전에는 발송·인증을 허용하지 않습니다. 다음 버튼은 입력값만 검사합니다.
  return (
    <section className="px-6 py-8">
      <Heading>계정을 만들어 주세요.</Heading>
      <Description className="mt-2">
        입력한 이메일로 인증번호를 보내드려요.
      </Description>

      <div className="mt-6 flex flex-col gap-5">
        <div className="flex flex-col gap-2">
          <SignupField
            id="signup-email"
            label="이메일"
            type="email"
            value={email}
            onChange={(value) => {
              setEmail(value)
              setEmailError('')
            }}
            error={emailError || undefined}
            placeholder="moa@example.com"
            autoComplete="email"
            inputMode="email"
          />
          <Button disabled className="button button--secondary w-full font-normal">
            인증번호 보내기
          </Button>
        </div>

        <div className="flex flex-col gap-2">
          <SignupField
            id="signup-code"
            label="인증번호"
            value={code}
            onChange={(value) => setCode(value.replace(/[^0-9]/g, ''))}
            placeholder="6자리 인증번호"
            autoComplete="one-time-code"
            inputMode="numeric"
            maxLength={6}
            action={
              <Button disabled className="button button--primary shrink-0 font-normal">
                인증하기
              </Button>
            }
          />
          <div className="flex items-center gap-1">
            <Description className="text-muted">인증번호를 받지 못했나요?</Description>
            <Button disabled className="text-caption text-[#426B45] disabled:cursor-not-allowed disabled:opacity-50">
              재전송
            </Button>
          </div>
        </div>

        <div className="border-t border-[#E5E5E5] pt-4">
          <SignupField
            id="signup-password"
            label="비밀번호"
            type="password"
            value={password}
            onChange={(value) => {
              setPassword(value)
              setPasswordError('')
            }}
            placeholder="비밀번호를 입력해 주세요."
            autoComplete="new-password"
            hint="비밀번호는 8자 이상으로 설정해 주세요."
            error={passwordError || undefined}
          />
        </div>

        <SignupField
          id="signup-password-confirm"
          label="비밀번호 확인"
          type="password"
          value={passwordConfirm}
          onChange={(value) => {
            setPasswordConfirm(value)
            setPasswordError('')
          }}
          placeholder="비밀번호를 다시 입력해 주세요."
          autoComplete="new-password"
          hint="비밀번호를 한 번 더 입력해 주세요."
        />

        <Button disabled={!canContinue} onClick={handleNext} className="button button--primary w-full font-normal">
          다음
        </Button>
      </div>
    </section>
  )
}
