import { useRef, useState } from 'react'
import { useNavigate } from 'react-router'
import { signup, SignupError, checkEmailAvailability, } from '../../api/AuthApi'
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
  const navigate = useNavigate()
  const submittingRef = useRef(false)
  const [isSubmitting, setIsSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState('')
  const [checkedEmail, setCheckedEmail] = useState<string | null>(null)
  const [isCheckingEmail, setIsCheckingEmail] = useState(false)
  const emailCheckIdRef = useRef(0)
  const [codeSent, setCodeSent] = useState(false)
  const [codeError, setCodeError] = useState('')
  const [verificationToken, setVerificationToken] = useState<string | null>(null)
  const [verificationAction, setVerificationAction] =
    useState<'send' | 'verify' | null>(null)
  const verificationRequestIdRef = useRef(0)

  const isVerificationBusy = verificationAction !== null
  const canSendCode =
    !!email &&
    checkedEmail === email &&
    !isCheckingEmail &&
    !isVerificationBusy &&
    !isSubmitting &&
    !verificationToken

  const canVerifyCode = canSendCode && codeSent && /^[0-9]{6}$/.test(code)

  async function handleCheckEmail() {
    if (isCheckingEmail || isSubmitting) return

    setCheckedEmail(null)
    setEmailError('')
    setSubmitError('')

    if (
      email.length > 50 ||
      /\s/.test(email) ||
      !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
    ) {
      setEmailError('올바른 이메일을 50자 이내로 입력해 주세요.')
      return
    }

    const requestId = ++emailCheckIdRef.current
    const requestedEmail = email
    setIsCheckingEmail(true)

    try {
      const result = await checkEmailAvailability(requestedEmail)

      // 확인 도중 이메일이 변경됐다면 이전 결과는 사용하지 않습니다.
      if (requestId !== emailCheckIdRef.current) return

      if (result.available) {
        setCheckedEmail(requestedEmail)
      } else {
        setEmailError('이미 가입된 이메일입니다.')
      }
    } catch (error) {
      if (requestId !== emailCheckIdRef.current) return

      setEmailError(
        error instanceof SignupError
          ? error.message
          : '이메일 중복 확인에 실패했어요. 다시 시도해 주세요.',
      )
    } finally {
      if (requestId === emailCheckIdRef.current) {
        setIsCheckingEmail(false)
      }
    }
  }

  async function handleSendCode() {
    if (!canSendCode) return

    const requestId = ++verificationRequestIdRef.current
    setVerificationAction('send')
    setCodeError('')
    setSubmitError('')

    try {
      await sendEmailVerification(email)

      if (requestId !== verificationRequestIdRef.current) return

      setCodeSent(true)
      setCode('')
    } catch (error) {
      if (requestId !== verificationRequestIdRef.current) return

      setCodeError(
        error instanceof SignupError
          ? error.message
          : '인증번호를 보내지 못했어요. 다시 시도해 주세요.',
      )
    } finally {
      if (requestId === verificationRequestIdRef.current) {
        setVerificationAction(null)
      }
    }
  }

  async function handleVerifyCode() {
    if (!canVerifyCode) return

    const requestId = ++verificationRequestIdRef.current
    setVerificationAction('verify')
    setCodeError('')
    setSubmitError('')

    try {
      const token = await verifyEmailCode(email, code)

      if (requestId !== verificationRequestIdRef.current) return

      setVerificationToken(token)
    } catch (error) {
      if (requestId !== verificationRequestIdRef.current) return

      setCodeError(
        error instanceof SignupError
          ? error.message
          : '인증번호를 확인하지 못했어요. 다시 시도해 주세요.',
      )
    } finally {
      if (requestId === verificationRequestIdRef.current) {
        setVerificationAction(null)
      }
    }
  }

  async function handleNext() {
    if (submittingRef.current) return
    if (!verificationToken) {
      setCodeError('이메일 인증을 완료해 주세요.')
      return
    }

    setSubmitError('')

    const emailMessage =
      !email
        ? '이메일을 입력해 주세요.'
        : email.length > 50 ||
          /\s/.test(email) ||
          !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
          ? '올바른 이메일을 50자 이내로 입력해 주세요.'
          : ''

    const passwordMessage =
      password.length < 8 ||
        password.length > 20 ||
        !/^(?=.*[A-Za-z])(?=.*[0-9])[\x21-\x7E]+$/.test(password)
        ? '비밀번호는 영문과 숫자를 포함해 공백 없이 8~20자로 입력해 주세요.'
        : password !== passwordConfirm
          ? '비밀번호가 일치하지 않습니다.'
          : ''

    setEmailError(emailMessage)
    setPasswordError(passwordMessage)

    if (emailMessage || passwordMessage) return

    if (checkedEmail !== email) {
      setEmailError('이메일 중복 확인을 해 주세요.')
      return
    }

    submittingRef.current = true
    setIsSubmitting(true)

    try {
      await signup({
        loginId: email,
        password,
        verificationToken,
      })

      navigate('/login', {
        replace: true,
        state: { signupCompleted: true },
      })
    } catch (error) {
      if (
        error instanceof SignupError &&
        error.code === 'INVALID_EMAIL_VERIFICATION_TOKEN'
      ) {
        setVerificationToken(null)
        setCodeSent(false)
        setCode('')
        setCodeError(error.message)
        return
      }
      setSubmitError(
        error instanceof SignupError
          ? error.message
          : '회원가입하지 못했어요. 잠시 후 다시 시도해 주세요.',
      )
    } finally {
      submittingRef.current = false
      setIsSubmitting(false)
    }
  }

  // 이메일 인증번호 발송·검증은 아직 연결하지 않습니다.
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
              if (submittingRef.current) return

              setEmail(value)
              setEmailError('')
              setSubmitError('')
              setCheckedEmail(null)

              emailCheckIdRef.current += 1
              setIsCheckingEmail(false)

              verificationRequestIdRef.current += 1
              setVerificationAction(null)
              setCodeSent(false)
              setCode('')
              setCodeError('')
              setVerificationToken(null)

            }}
            error={emailError || undefined}
            placeholder="moa@example.com"
            autoComplete="email"
            inputMode="email"
            action={
              <Button
                type="button"
                disabled={!email || isCheckingEmail || isSubmitting}
                onClick={() => void handleCheckEmail()}
                className="button button--secondary shrink-0 whitespace-nowrap font-normal"
                style={{ minHeight: 'var(--button-height-primary)' }}
              >
                {isCheckingEmail ? '확인 중…' : '중복 확인'}
              </Button>
            }
          />

          {checkedEmail === email && (
            <p role="status" className="text-caption text-[var(--color-primary)]">
              사용 가능한 이메일입니다.
            </p>
          )}

          <Button disabled className="button button--secondary w-full font-normal">
            인증번호 보내기
          </Button>
        </div>

        <div className="flex flex-col gap-2">
          <SignupField
            id="signup-code"
            label="인증번호"
            value={code}
            onChange={(value) => {
              if (isVerificationBusy || isSubmitting || verificationToken) return

              setCode(value.replace(/[^0-9]/g, '').slice(0, 6))
              setCodeError('')
            }} placeholder="6자리 인증번호"
            autoComplete="one-time-code"
            inputMode="numeric"
            maxLength={6}
            action={
              <Button disabled className="button button--primary shrink-0 font-normal">
                인증하기
              </Button>
            }
          />
          
          {(verificationToken || codeSent) && (
            <p role="status" className="text-caption text-[var(--color-primary)]">
              {verificationToken
                ? '이메일 인증이 완료됐어요.'
                : '인증번호를 보냈어요. 5분 안에 입력해 주세요.'}
            </p>
          )}

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

        {submitError && (
          <p role="alert" className="text-caption text-[var(--color-error)]">
            {submitError}
          </p>
        )}

        <Button
          disabled={isSubmitting}
          onClick={() => void handleNext()}
          className="button button--primary w-full font-normal"
        >
          {isSubmitting ? '가입 중…' : '회원가입'}
        </Button>
      </div>
    </section>
  )
}
