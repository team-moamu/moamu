import { Link } from 'react-router'

export function AccountSecurityPage() {
  return (
    <div className="space-y-6 px-6 py-6">
      <section aria-labelledby="account-info-heading">
        <h2 id="account-info-heading" className="mb-3 font-bold">
          가입 정보
        </h2>
        <dl className="divide-y divide-neutral-200 rounded-2xl bg-white border border-[#E8CFAC]">
          <div className="flex items-center justify-between gap-3 px-4 py-4">
            <div className="min-w-0">
              <dt className="font-medium">가입 이메일</dt>
              <dd className="mt-1 break-all text-sm text-neutral-600">
                moa@example.com
              </dd>
            </div>
            <span className="shrink-0 text-xs text-accent">인증 완료</span>
          </div>
          <div className="px-4 py-4">
            <dt className="font-medium">로그인 방식</dt>
            <dd className="mt-1 text-sm text-neutral-600">이메일과 비밀번호</dd>
          </div>
        </dl>
      </section>

      <section aria-labelledby="security-heading">
        <h2 id="security-heading" className="mb-3 font-bold">
          보안 설정
        </h2>
        <Link
          to="/mypage/password-reset"
          className="flex items-center justify-between gap-3 rounded-2xl border border-[#E8CFAC] bg-white px-4 py-4"
        >
          <div>
            <p className="font-medium">비밀번호 변경</p>
            <p className="mt-1 text-sm text-neutral-600">
              비밀번호를 새로 설정해요.
            </p>
          </div>
          <span aria-hidden="true">›</span>
        </Link>
      </section>

      <section aria-labelledby="account-management-heading">
        <h2 id="account-management-heading" className="mb-3 font-bold">
          계정 관리
        </h2>
        <p className="rounded-2xl bg-white px-4 py-4 font-medium border border-[#E8CFAC]">로그아웃</p>
        <p className="mt-3 text-center text-sm text-neutral-400">
          회원탈퇴를 하시겠습니까?
        </p>
      </section>
    </div>
  )
}