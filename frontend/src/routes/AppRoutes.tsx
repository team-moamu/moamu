
import { Route, Routes } from 'react-router'
import { MainLayout } from '../components/layout/MainLayout'
import { ChallengeCreatePage } from '../pages/challenge/ChallengeCreatePage'

function HomePage() {
  return <h1>모아뮤 홈 화면</h1>
}

function ChallengePage() {
  return <h1>도전하기 화면</h1>
}

function GalleryPage() {
  return <h1>미술관 화면</h1>
}

function CollectionPage() {
  return <h1>보관함 화면</h1>
}

function MyPage() {
  return <h1>마이페이지 화면</h1>
}

export default function AppRoutes() {
  return (
    <Routes>
      {/* 
        - element에 showBottomNav 추가하면 네비게이션바가 생깁니다. 
        - headerType: MainLayout.tsx에 있는 헤더 타입
      */}
      {/* 기본 헤더 + 하단 내비게이션 */}
      <Route element={<MainLayout showBottomNav />}>
        <Route path="/" element={<HomePage />} />
        <Route path="/collection" element={<CollectionPage />} />
        <Route path="/mypage" element={<MyPage />} />
      </Route>

      {/* 뒤로가기 헤더 */}
      <Route element={<MainLayout headerType="back" title="도전 생성" />}>
        <Route path="/challenge/create" element={<ChallengeCreatePage />} />
      </Route>

      <Route element={<MainLayout showBottomNav headerType="back" title="도전" />}>
        <Route path="/challenge" element={<ChallengePage />} />
      </Route>

      {/* 제목 헤더 */}
      <Route element={<MainLayout showBottomNav headerType="title" title="미술관" />} >
        <Route path="/challenge/complete" element={<GalleryPage />} />
      </Route>

      {/* 로그인 헤더 */}
      <Route element={<MainLayout headerType="login" />}>
        <Route path="/login" element={<HomePage />} />
      </Route>
    </Routes>
  )
}