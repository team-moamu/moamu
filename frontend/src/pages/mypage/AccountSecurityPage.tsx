import { useRef, useState } from 'react'
import { Link } from 'react-router'
import { logout } from '../../api/AuthApi'
import { clearAccessToken, getLoginEmail } from '../../auth/AuthStorage'

export function AccountSecurityPage() {
  const loginEmail = getLoginEmail()
  const logoutInProgress = useRef(false)
  const [isLoggingOut, setIsLoggingOut] = useState(false)
  const [logoutError, setLogoutError] = useState('')

  async function handleLogout() {
    if (logoutInProgress.current) return

    logoutInProgress.current = true
    setIsLoggingOut(true)
    setLogoutError('')

    try {
      await logout()
      clearAccessToken()
      window.location.replace('/login')
    } catch {
      setLogoutError('로그아웃하지 못했어요. 잠시 후 다시 시도해 주세요.')
      logoutInProgress.current = false
      setIsLoggingOut(false)
    }
  }
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
                {loginEmail ?? '로그인 후 이메일을 확인할 수 있어요.'}
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
        <button
          type="button"
          disabled={isLoggingOut}
          onClick={() => void handleLogout()}
          className="flex w-full items-center justify-between rounded-2xl border border-[#E8CFAC] bg-white px-4 py-4 text-left font-medium disabled:opacity-50"
        >
          <span>{isLoggingOut ? '로그아웃 중…' : '로그아웃'}</span>
          <span aria-hidden="true">›</span>
        </button>

        {logoutError && (
          <p role="alert" className="mt-2 text-sm text-[var(--color-error)]">
            {logoutError}
          </p>
        )}
        <Link
          to="/mypage/withdrawal"
          className="mt-3 block text-center text-sm text-neutral-400 underline underline-offset-4"
        >
          회원탈퇴를 하시겠습니까?
        </Link>
      </section>
    </div>
  )
}