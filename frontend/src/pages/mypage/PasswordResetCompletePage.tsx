import { Link, Navigate, useLocation } from 'react-router'

export function PasswordResetCompletePage() {
  
  const { state } = useLocation()

  if (state?.resetCompleted !== true) {
    return <Navigate to="/login" replace />
  }
  return (
    <div className="flex flex-1 flex-col px-6 pb-28 text-[#493725]">
      <section className="flex flex-col items-center pt-[160px] text-center">
        <div
          aria-hidden="true"
          className="grid h-[100px] w-[100px] place-items-center rounded-full bg-[#E4EBD5]"
        >
          <span className="h-5 w-9 -translate-y-1 rotate-[-45deg] border-b-[6px] border-l-[6px] border-[#426B45]" />
        </div>

        <h2 className="mt-6 font-[family-name:var(--font-title)] text-[24px] leading-[36px]">
          비밀번호가
          <br />
          변경됐어요
        </h2>

        <p className="mt-3 text-[12px] leading-[18px]">
          새 비밀번호로 다시 로그인해 주세요.
        </p>

        <div className="mt-6 w-full rounded-[12px] border border-[#E8CFAC] p-4 text-left">
          <h3 className="text-[14px] font-bold">변경 완료</h3>
          <p className="mt-1 text-[12px] leading-[18px]">
            로그인 화면으로 이동해 새 비밀번호를 입력하세요.
          </p>
        </div>
      </section>

      <div className="fixed inset-x-0 bottom-0 z-10 mx-auto w-full max-w-[430px] bg-white px-6 pt-4 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
        <Link
          to="/login"
          replace
          className="button button--primary button--full h-[52px]"
        >
          로그인으로 돌아가기
        </Link>
      </div>
    </div>
  )
}