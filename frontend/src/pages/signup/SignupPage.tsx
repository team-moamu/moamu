
import { useState } from 'react'
import { AuthLink } from '../../components/atoms/link/AuthLink'
import { Button } from '../../components/atoms/button/Button'
import { SignupIntro } from '../../components/molecules/signup/SignupIntro'
import { TermsModal } from '../../components/molecules/modal/TermsModal'

export function SignupPage() {
  const [isTermsOpen, setIsTermsOpen] = useState(false)
  return (
    <section className="px-6 py-8">
      {/* 소개 */}
      <SignupIntro />

      {/* 가입 방법 */}
      <div className="mt-14 flex flex-col gap-3">
        <Button
          type="button"
          onClick={() => setIsTermsOpen(true)}
          className="button button--primary w-full text-body font-normal"
        >
          이메일로 가입하기
        </Button>
      </div>

      {/* 로그인 및 안내*/}
      <div className="mt-7 text-center"> 
        <AuthLink to="/login">이미 계정이 있나요? 로그인</AuthLink>
      </div>
      
      {isTermsOpen && (
      <TermsModal
          onClose={() => setIsTermsOpen(false)}
          onAgree={(marketingAgreed) => {
          // 다음 단계에서 동의 정보를 보관하고 이메일 입력 화면으로 이동합니다.
          console.log('마케팅 수신 동의:', marketingAgreed)
          setIsTermsOpen(false)
          }}
      />
      )}

    </section>
  )
}
