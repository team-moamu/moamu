
import { Route, Routes, useLocation } from 'react-router'
import { getMe } from '../api/AuthApi'
import { MainLayout } from '../components/layout/MainLayout'
import { ChallengeCreatePage } from '../pages/challenge/ChallengeCreatePage'
import { ChallengeListPage } from '../pages/challenge/ChallengeListPage'
import { ChallengeDetailPage } from '../pages/challenge/ChallengeDetailPage'
import { VerificationPage } from '../pages/challenge/VerificationPage'
import { LoginPage } from '../pages/auth/LoginPage'
import { SignupPage } from '../pages/signup/SignupPage'
import { EmailSignupPage } from '../pages/signup/EmailSignupPage'
import { VerificationResultPage } from '../pages/challenge/VerificationResultPage'
import { HomePage } from '../pages/home/HomePage'
import { ChallengeCompletionPage } from '../pages/challenge/ChallengeCompletionPage'
import { GoalVerificationConditionsPage } from '../pages/challenge/GoalVerificationConditionsPage'
import { GoalVerificationUnavailablePage } from '../pages/challenge/GoalVerificationUnavailablePage'
import { GalleryDesktopPage } from '../pages/gallery/GalleryDesktopPage'
import { GalleryPage } from '../pages/gallery/GalleryPage'
import { GalleryDetailPage } from '../pages/gallery/GalleryDetailPage'
import { CollectionPage } from '../pages/collection/CollectionPage'
import { MyPage } from '../pages/mypage/MyPage'
import { AccountSecurityPage } from '../pages/mypage/AccountSecurityPage'
import { ProfileEditPage } from '../pages/mypage/ProfileEditPage'
import { PasswordResetPage } from '../pages/mypage/PasswordResetPage'
import { NewPasswordPage } from '../pages/mypage/NewPasswordPage'
import { useEffect, useState } from 'react'
import { WithdrawalPage } from '../pages/mypage/WithdrawalPage'
import { WithdrawalConfirmPage } from '../pages/mypage/WithdrawalConfirmPage'
import { WithdrawalCompletePage } from '../pages/mypage/WithdrawalCompletePage'
import { PasswordResetCompletePage } from '../pages/mypage/PasswordResetCompletePage'

function ChallengePage() {
  return <h1>도전하기 화면</h1>
}

export default function AppRoutes() {
  const { pathname } = useLocation()
  const isMyPageRoute = pathname === '/mypage' || pathname.startsWith('/mypage/')
  const [nickname, setNickname] = useState('')
  const [email, setEmail] = useState<string | null>(null)
  const [profilePhoto, setProfilePhoto] = useState<File | null>(null)
  const [profilePhotoUrl, setProfilePhotoUrl] = useState<string | null>(null)

  useEffect(() => {
    if (!isMyPageRoute) {
      setNickname('')
      setEmail(null)
      return
    }

    const controller = new AbortController()

    getMe(controller.signal)
      .then((user) => {
        if (!controller.signal.aborted) {
          setNickname(user.name)
          setEmail(user.email)
        }
      })
      .catch(() => {
        if (!controller.signal.aborted) {
          setNickname('')
          setEmail(null)
        }
      })

    return () => controller.abort()
  }, [isMyPageRoute])

  useEffect(() => {
    if (!profilePhoto) {
      setProfilePhotoUrl(null)
      return
    }

    const url = URL.createObjectURL(profilePhoto)
    setProfilePhotoUrl(url)

    return () => URL.revokeObjectURL(url)
  }, [profilePhoto])

  return (
    <Routes>
      <Route path="/museum/desktop" element={<GalleryDesktopPage />} />
      {/* 
        - element에 showBottomNav 추가하면 네비게이션바가 생깁니다. 
        - headerType: MainLayout.tsx에 있는 헤더 타입
        - background='white' => 배경색
      */}
      {/* 기본 헤더 + 하단 내비게이션 */}
      <Route
        element={
          <MainLayout showBottomNav profilePhotoUrl={profilePhotoUrl} />
        }
      >
        <Route path="/" element={<HomePage />} />
      </Route>

      {/* 마이페이지 헤더 + 하단 내비게이션 */}
      <Route
        path="/mypage"
        element={
          <MyPage
            nickname={nickname || '회원'}
            email={email}
            profilePhotoUrl={profilePhotoUrl}
          />
        }
      />

      <Route element={<MainLayout headerType="back" title="계정 및 보안" />}>
        <Route path="/mypage/account-security" element={<AccountSecurityPage email={email} />} />
      </Route>

      <Route
        element={
          <MainLayout
            headerType="back"
            title="회원 탈퇴"
            background="white"
          />
        }
      >
        <Route path="/mypage/withdrawal" element={<WithdrawalPage />} />
        <Route
          path="/mypage/withdrawal/confirm"
          element={<WithdrawalConfirmPage />}
        />
        <Route
          path="/mypage/withdrawal/complete"
          element={<WithdrawalCompletePage />}
        />
      </Route>

      <Route
        element={
          <MainLayout
            headerType="back"
            title="비밀번호 재설정"
            background="white"
          />
        }
      >
        <Route path="/mypage/password-reset" element={<PasswordResetPage />} />
      </Route>

      <Route
        element={
          <MainLayout
            headerType="back"
            title="새 비밀번호 설정"
            background="white"
          />
        }
      >
        <Route path="/mypage/password-reset/new" element={<NewPasswordPage />} />
      </Route>

      <Route
        element={
          <MainLayout
            headerType="title"
            title="변경 완료"
            background="white"
          />
        }
      >
        <Route
          path="/mypage/password-reset/complete"
          element={<PasswordResetCompletePage />}
        />
      </Route>

      <Route element={<MainLayout headerType="back" title="프로필 수정" />}>
        <Route
          path="/mypage/profile"
          element={
            <ProfileEditPage
              nickname={nickname}
              profilePhoto={profilePhoto}
              onSave={(nextNickname, nextPhoto) => {
                setNickname(nextNickname)
                setProfilePhoto(nextPhoto)
              }}
            />
          }
        />
      </Route>

      {/* 뒤로가기 헤더 */}
      <Route element={<MainLayout headerType="back" title="도전 생성" />}>
        <Route path="/challenge/create" element={<ChallengeCreatePage />} />
      </Route>

      <Route element={<MainLayout headerType="back" title="인증 조건 확인" background="white" />}>
        <Route path="/challenge/create/conditions" element={<GoalVerificationConditionsPage />} />
        <Route path="/challenge/create/conditions/unverifiable" element={<GoalVerificationUnavailablePage />} />
      </Route>

      <Route element={<MainLayout showBottomNav headerType="back" title="도전" />}>
        <Route path="/challenge" element={<ChallengePage />} />
      </Route>

      {/* 제목 헤더 */}
      <Route element={<MainLayout showBottomNav headerType="title" title="보관함" />}>
        <Route path="/collection" element={<CollectionPage />} />
      </Route>

      <Route element={<MainLayout showBottomNav headerType="title" title="내 미술관" />}>
        <Route path="/museum" element={<GalleryPage />} />
        <Route path="/challenge/complete" element={<GalleryPage />} />
      </Route>

      <Route element={<MainLayout showBottomNav background="white" headerType="back" title="내 미술관" />}>
        <Route path="/museum/:challengeId" element={<GalleryDetailPage />} />
      </Route>

      <Route element={<MainLayout showBottomNav background='white' headerType="title" title="도전하기" />} >
        <Route path="/challenge/list" element={<ChallengeListPage />} />
      </Route>

      <Route element={<MainLayout showBottomNav background="white" headerType="title" />}>
        <Route path="/challenge/:challengeId" element={<ChallengeDetailPage />} />
      </Route>

      <Route element={<MainLayout showBottomNav background="white" headerType="title" title='오늘의 인증' />}>
        <Route path="/challenge/:challengeId/verification" element={<VerificationPage />} />
      </Route>

      {/* 로그인 헤더 */}
      <Route element={<MainLayout headerType="login" background="white" />}>
        <Route path="/login" element={<LoginPage />} />
      </Route>

      {/* 회원가입 */}
      <Route
        element={
          <MainLayout
            headerType="back"
            title="이메일 회원가입"
            background="white"
          />
        }
      >
        <Route path="/signup" element={<SignupPage />} />
      </Route>

      {/* 이메일 회원가입 */}
      <Route
        element={
          <MainLayout
            headerType="back"
            title="이메일 회원가입"
            background="white"
          />
        }
      >
        <Route path="/signup/email" element={<EmailSignupPage />} />
      </Route>
      {/* 자체 레이아웃: 화면 상태에 따라 헤더와 하단 내비게이션 구성 */}
      {/* 도전 전체 완료: 축하 화면을 먼저 표시한 뒤 결과 화면으로 자동 전환 */}
      <Route path="/challenge/:challengeId/completion" element={<ChallengeCompletionPage />} />
      {/* 인증 결과: 실패·검토 화면에만 하단 내비게이션 표시 */}
      <Route path="/challenge/:challengeId/verification/result/:verificationId" element={<VerificationResultPage />} />
    </Routes >
  )
}
