import { useState } from 'react'
import { Link, useNavigate } from 'react-router'

export function WithdrawalPage() {
  const navigate = useNavigate()
  const [agreed, setAgreed] = useState(false)

  return (
    <div className="px-6 pt-6 pb-48 text-[#493725]">
      <h2 className="font-[family-name:var(--font-title)] text-[24px] leading-[36px]">
        떠나기 전에 확인해 주세요.
      </h2>
      <p className="mt-2 text-[12px] leading-[18px]">
        탈퇴 후에는 이 계정과 도전 기록을 다시 이용할 수 없어요.
      </p>

      <section
        aria-labelledby="withdrawal-notice-heading"
        className="mt-9 min-h-[196px] rounded-[12px] bg-[#FFF8EC] p-6"
      >
        <h3
          id="withdrawal-notice-heading"
          className="text-[16px] leading-6 font-bold"
        >
          탈퇴 전 확인해주세요.
        </h3>

        <ul className="mt-3 list-disc space-y-3 pl-4 text-[12px] leading-[18px]">
          <li>계정과 프로필을 다시 이용할 수 없어요.</li>
          <li>도전 기록과 작품은 복구할 수 없어요.</li>
          <li>진행 중인 도전은 이어갈 수 없어요.</li>
          <li>이전 도전 기록과 작품은 새 계정으로 이전되지 않아요.</li>
          <li>같은 이메일로 다시 가입할 수 있어요. </li>
        </ul>

        <p className="mt-5 text-[12px] leading-[18px] text-[var(--color-primary)]">
          회원 탈퇴 시 정보 처리 안내
        </p>
      </section>

      <label className="mt-10 flex cursor-pointer items-start justify-center gap-2 text-[12px] leading-[18px]">
        <input
          type="checkbox"
          checked={agreed}
          onChange={(event) => setAgreed(event.currentTarget.checked)}
          className="mt-0.5 h-4 w-4 shrink-0 accent-[var(--color-primary)]"
        />
        <span>안내 사항을 확인했으며 탈퇴에 동의해요.</span>
      </label>

      <div className="fixed inset-x-0 bottom-0 z-10 mx-auto flex w-full max-w-[430px] flex-col gap-4 bg-white px-6 pt-4 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
        <Link
          to="/mypage/account-security"
          className="button button--primary button--full h-[52px]"
        >
          계속 이용하기
        </Link>

        <button
          type="button"
          disabled={!agreed}
          onClick={() => navigate('/mypage/withdrawal/confirm')}
          className="button button--primary button--full h-[52px]"
        >
          회원 탈퇴
        </button>
      </div>
    </div>
  )
}