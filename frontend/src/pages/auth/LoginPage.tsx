import { useState } from 'react'
import { Button } from '../../components/atoms/button/Button'
import { Input } from '../../components/atoms/input/Input'
import { Text } from '../../components/atoms/typography/Text'
import { LabeledInput } from '../../components/molecules/form/LabeledInput'
import { SocialLoginButton } from '../../components/molecules/form/SocialLoginButton'
import { login, LoginError } from '../../api/AuthApi'
import { Link, useLocation, useNavigate } from 'react-router'
import { saveAccessToken } from '../../auth/authStorage'

export function LoginPage() {
  const navigate = useNavigate()

  const location = useLocation()
  const signupCompleted = location.state?.signupCompleted === true

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [autoLogin, setAutoLogin] = useState(false)

  const [isSubmitting, setIsSubmitting] = useState(false)
  const [errorMessage, setErrorMessage] = useState('')

  async function handleLogin() {
    if (isSubmitting) return

    setIsSubmitting(true)
    setErrorMessage('')

    try {
      const result = await login({
        loginId: email,
        password,
      })

      saveAccessToken(result.accessToken)

      navigate('/', { replace: true })
    } catch (error) {
      if (error instanceof LoginError) {
        switch (error.code) {
          case 'INVALID_CREDENTIALS':
            setErrorMessage('이메일 또는 비밀번호를 확인해 주세요.')
            break

          case 'LOGIN_NOT_ALLOWED':
            setErrorMessage('로그인할 수 없는 계정입니다.')
            break

          default:
            setErrorMessage(error.message)
        }
      } else {
        setErrorMessage('잠시 후 다시 시도해 주세요.')
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <section className="px-6 py-6">
      <h1 className="mb-6">
        <Text className="text-description--header">
          작은 습관이 모여,
          <br />
          나만의 미술관으로
        </Text>
      </h1>

      {signupCompleted && (
        <p
          role="status"
          className="text-caption mb-4 text-[var(--color-primary)]"
        >
          회원가입이 완료됐어요. 가입한 정보로 로그인해 주세요.
        </p>
      )}

      <form
        className="flex flex-col gap-3"
        aria-busy={isSubmitting}
        onSubmit={(event) => {
          event.preventDefault()
          void handleLogin()
        }}
      >
        <LabeledInput
          id="login-email"
          name="loginId"
          label="이메일"
          type="email"
          autoComplete="username"
          placeholder="이메일을 입력해 주세요"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          className="min-h-[52px] rounded-xl px-4 text-base"
          disabled={isSubmitting}
          required
        />

        <LabeledInput
          id="login-password"
          name="password"
          label="비밀번호"
          type="password"
          autoComplete="current-password"
          placeholder="비밀번호를 입력해 주세요"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
          className="min-h-[52px] rounded-xl px-4 text-base"
          disabled={isSubmitting}
          required
        />

        <label className="flex w-fit cursor-pointer items-center gap-2">
          <Input
            name="autoLogin"
            type="checkbox"
            checked={autoLogin}
            onChange={(event) => {
              setAutoLogin(event.target.checked)
            }}
            className="size-3 accent-[var(--color-primary)]"
          />

          <span className="text-caption">
            자동 로그인
          </span>
        </label>

        {errorMessage && (
          <p role="alert" className="field__error">
            {errorMessage}
          </p>
        )}

        <Button
          type="submit"
          disabled={isSubmitting}
          className="
            button button--primary button--full
            mt-1 text-base font-normal
          "
        >
          {isSubmitting ? '로그인 중…' : '로그인'}
        </Button>
      </form>

      <div className="mt-3 flex justify-center">
        <Button
          disabled
          className="
            text-caption text-[var(--color-primary)]
            disabled:cursor-not-allowed
          "
        >
          비밀번호 찾기
        </Button>
      </div>

      <div className="mt-3">
        <p className="text-caption text-muted mb-5 text-center">
          또는 구글 계정으로 시작하기
        </p>

        <SocialLoginButton disabled />
      </div>

      <div className="mt-5 flex justify-center">
        <Link
          to="/signup"
          className="text-caption text-[var(--color-primary)]"
        >
          처음 오셨나요? 이메일 회원가입
        </Link>
      </div>
    </section>
  )
}