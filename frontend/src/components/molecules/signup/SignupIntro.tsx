import { Heading } from '../../atoms/typography/Heading'
import { Description } from '../../atoms/typography/Description'

export function SignupIntro() {
  return (
    <div>
      <Heading>모아뮤를 시작해요.</Heading>
      <Description className="mt-2">
        이메일 또는 소셜로 가입할 수 있어요.
      </Description>
    </div>
  )
}
