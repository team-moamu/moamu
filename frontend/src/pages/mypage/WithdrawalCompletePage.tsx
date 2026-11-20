import { Link } from 'react-router'

export function WithdrawalCompletePage() {
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
          탈퇴가 완료됐어요
        </h2>

        <p className="mt-2 text-[12px] leading-6">
          그동안 모아뮤와 함께해주셔서 감사해요.
          <br />
          다시 만날 날을 기다릴게요.
        </p>
      </section>

      <div className="fixed inset-x-0 bottom-0 z-10 mx-auto w-full max-w-[430px] bg-white px-6 pt-4 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
        <Link
          to="/"
          replace
          className="button button--primary button--full h-[52px]"
        >
          시작 화면으로
        </Link>
      </div>
    </div>
  )
}