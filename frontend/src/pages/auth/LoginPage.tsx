import { useState } from 'react'
import { Button } from '../../components/atoms/button/Button'
import { Input } from '../../components/atoms/input/Input'
import { Text } from '../../components/atoms/typography/Text'
import { LabeledInput } from '../../components/molecules/form/LabeledInput'
import { SocialLoginButton } from '../../components/molecules/form/SocialLoginButton'

export function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [autoLogin, setAutoLogin] = useState(false)

  return (
    <section className="px-6 py-6">
      <h1 className="mb-6">
        <Text className="text-description--header">
          작은 습관이 모여,
          <br />
          나만의 미술관으로
        </Text>
      </h1>

      <form
        className="flex flex-col gap-3"
        onSubmit={(event) => {
          event.preventDefault()
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

        <Button
          type="submit"
          className="
            button button--primary button--full
            mt-1 text-base font-normal
          "
        >
          로그인
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
        <Button
          disabled
          className="
            text-caption text-[var(--color-primary)]
            disabled:cursor-not-allowed
          "
        >
          처음 오셨나요? 이메일 회원가입
        </Button>
      </div>
    </section>
  )
}