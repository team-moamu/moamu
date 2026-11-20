import { useEffect, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import defaultProfileMu from '../../assets/images/default_profile_mu.png'
import { getLoginEmail } from '../../auth/authStorage'

const activityCounts = [
  { label: '진행 중', count: 2 },
  { label: '완료', count: 4 },
  { label: '중단', count: 1 },
]

const exhibitionCount = 2

type MyPageProps = {
  nickname: string
  profilePhotoUrl: string | null
}

export function MyPage({ nickname, profilePhotoUrl }: MyPageProps) {
  const loginEmail = getLoginEmail()
  const location = useLocation()
  const navigate = useNavigate()
  const [showSavedMessage, setShowSavedMessage] = useState(
    location.state?.profileSaved === true,
  )

  useEffect(() => {
    if (!showSavedMessage) return

    // 뒤로가기나 새로고침으로 완료 안내가 다시 표시되지 않도록 처리해요.
    navigate('/mypage', { replace: true, state: null })

    const timer = window.setTimeout(() => {
      setShowSavedMessage(false)
    }, 3000)

    return () => window.clearTimeout(timer)
  }, [showSavedMessage, navigate])
  return (
    <div className="space-y-6 px-6 py-6">
      <section aria-label="프로필" className="flex items-center gap-4 rounded-2xl border border-[#E8CFAC] bg-white p-4">
        <img
          src={profilePhotoUrl ?? defaultProfileMu}
          alt=""
          className="h-16 w-16 shrink-0 rounded-full object-cover"
        />
        <div className="min-w-0">
          <p className="break-all text-xl font-bold">{nickname} 님</p>
          <p className="break-all text-sm">
            {loginEmail ?? '로그인 후 이메일을 확인할 수 있어요.'}
          </p>
          <Link
            to="/mypage/profile"
            className="mt-2 inline-block text-sm font-medium text-accent"
          >
            프로필 수정
          </Link>
        </div>
      </section>
      <section aria-labelledby="activity-heading">
        <div className="mb-3 flex items-center justify-between gap-3">
          <h2 id="activity-heading" className="text-lg font-bold">
            나의 활동
          </h2>
          <Link to="/collection" className="text-sm font-medium">
            보관함 보기 <span aria-hidden="true">›</span>
          </Link>
        </div>

        <ul className="grid grid-cols-3 divide-x divide-[#E8CFAC] rounded-2xl border border-[#E8CFAC] bg-white py-4 text-center">
          {activityCounts.map(({ label, count }) => (
            <li key={label}>
              <p className="text-2xl font-bold">{count}</p>
              <p className="mt-1 text-sm">{label}</p>
            </li>
          ))}
        </ul>
      </section>
      {/* 나의 전시 */}
      <section aria-labelledby="exhibition-heading">
        <div className="mb-3 flex items-center justify-between gap-3 ">
          <h2 id="exhibition-heading" className="text-lg font-bold">
            나의 전시
          </h2>

          <Link to="/museum" className="text-sm font-medium">
            전시관 보기 <span aria-hidden="true">›</span>
          </Link>
        </div>

        <div className="rounded-2xl border border-[#E8CFAC] bg-white p-4">
          <p className="text-sm">현재 전시 중인 작품</p>
          <p className="mt-1 text-lg font-bold">{exhibitionCount}점 전시 중</p>
        </div>
      </section>

      <section aria-labelledby="my-settings-heading">
        <h2 id="my-settings-heading" className="mb-3 text-lg font-bold">
          설정 및 안내
        </h2>

        <ul className="divide-y divide-[#E8CFAC] overflow-hidden rounded-2xl border border-[#E8CFAC] bg-white">
          <li>
            <Link
              to="/mypage/account-security"
              className="flex w-full items-center justify-between px-5 py-4 text-sm font-medium"
            >
              <span>계정 및 보안</span>
              <span aria-hidden="true">›</span>
            </Link>
          </li>

          <li className="px-5 py-4 text-sm font-medium">
            약관 및 개인정보
          </li>
        </ul>
      </section>
      {showSavedMessage && (
        <div className="pointer-events-none fixed inset-x-0 bottom-28 z-40 mx-auto w-full max-w-[430px] px-6">
          <p
            role="status"
            className="rounded-xl bg-[var(--color-primary)] px-4 py-3 text-center text-sm text-white shadow-lg"
          >
            프로필이 변경됐어요.
          </p>
        </div>
      )}
    </div>
  )
}