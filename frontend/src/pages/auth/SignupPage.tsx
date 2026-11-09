import { useState } from 'react'
import { Button } from '../../components/atoms/button/Button'
import { Text } from '../../components/atoms/typography/Text'
import { LabeledInput } from '../../components/molecules/form/LabeledInput'

export function SignupPage() {
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [errorMessage, setErrorMessage] = useState('')

  function handleSignup() {
    setErrorMessage('')

    if (!name.trim()) {
      setErrorMessage('이름을 입력해 주세요.')
      return
    }

    if (/\s/.test(email)) {
      setErrorMessage('이메일에는 공백을 포함할 수 없어요.')
      return
    }

    const passwordPattern = /^(?=.*[A-Za-z])(?=.*[0-9])[\x21-\x7E]+$/

    if (
      password.length < 8 ||
      password.length > 20 ||
      !passwordPattern.test(password)
    ) {
      setErrorMessage(
        '비밀번호는 영문과 숫자를 포함한 8~20자로 입력해 주세요. 공백은 사용할 수 없어요.',
      )
      return
    }

    if (password !== passwordConfirm) {
      setErrorMessage('비밀번호가 일치하지 않아요.')
      return
    }

    // 다음 단계에서 회원가입 API를 연결합니다.
  }

  return (
    <section className="px-6 py-6">
      <h1 className="mb-6">
        <Text className="text-description--header">
          모아뮤에 오신 것을 환영해요.
        </Text>
      </h1>

      <form
        className="flex flex-col gap-4"
        onSubmit={(event) => {
          event.preventDefault()
          handleSignup()
        }}
      >
        <LabeledInput
          id="signup-name"
          name="name"
          label="이름"
          type="text"
          autoComplete="name"
          placeholder="이름을 입력해 주세요"
          value={name}
          onChange={(event) => setName(event.target.value)}
          maxLength={100}
          className="min-h-[52px] rounded-xl px-4 text-base"
          required
        />

        <LabeledInput
          id="signup-email"
          name="loginId"
          label="이메일"
          type="email"
          autoComplete="email"
          placeholder="이메일을 입력해 주세요"
          value={email}
          onChange={(event) => setEmail(event.target.value)}
          maxLength={50}
          className="min-h-[52px] rounded-xl px-4 text-base"
          required
        />

        <div>
          <LabeledInput
            id="signup-password"
            name="password"
            label="비밀번호"
            type="password"
            autoComplete="new-password"
            placeholder="비밀번호를 입력해 주세요"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            minLength={8}
            maxLength={20}
            aria-describedby="signup-password-hint"
            className="min-h-[52px] rounded-xl px-4 text-base"
            required
          />

          <p
            id="signup-password-hint"
            className="text-caption text-muted mt-2"
          >
            영문과 숫자를 포함한 8~20자로 입력해 주세요.
            공백은 사용할 수 없어요.
          </p>
        </div>

        <LabeledInput
          id="signup-password-confirm"
          name="passwordConfirm"
          label="비밀번호 확인"
          type="password"
          autoComplete="new-password"
          placeholder="비밀번호를 다시 입력해 주세요"
          value={passwordConfirm}
          onChange={(event) => {
            setPasswordConfirm(event.target.value)
          }}
          minLength={8}
          maxLength={20}
          className="min-h-[52px] rounded-xl px-4 text-base"
          required
        />

        {errorMessage && (
          <p role="alert" className="field__error">
            {errorMessage}
          </p>
        )}

        <Button
          type="submit"
          className="
            button button--primary button--full
            mt-2 text-base font-normal
          "
        >
          회원가입
        </Button>
      </form>
    </section>
  )
}