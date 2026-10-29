
import { Link, Route, Routes } from 'react-router'
import { TitleHeader } from '../components/layout/header/TitleHeader'
import { LoginHeader } from '../components/layout/header/LoginHeader'
import { BackHeader } from '../components/layout/header/BackHeader'
import { MainHeader } from '../components/layout/header/MainHeader'

// 이 파일은 라우팅 테스트용 임시 페이지를 포함합니다.
// 실제 페이지 연결 시 임시 페이지 컴포넌트를 제거해 주세요.

function HomePage() {
  return <h1>모아뮤 홈 화면</h1>
}

function ChallengePage() {
  return <h1>도전하기 화면</h1>
}

function MuseumPage() {
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
    <div className="space-y-8 p-6">

      <Routes>
        <Route element={<MainHeader />}>
          <Route path="/" element={<HomePage />} />
          <Route path="/challenge" element={<ChallengePage />} />
          <Route path="/museum" element={<MuseumPage />} />
          <Route path="/collection" element={<CollectionPage />} />
          <Route path="/mypage" element={<MyPage />} />
        </Route>

        <Route element={<BackHeader title="도전 생성" />}>
          <Route path="/challenge/create" element={<ChallengePage />} />
        </Route>

        <Route element={<TitleHeader title="도전 생성" />}>
          
        </Route>

        <Route element={<LoginHeader />}>
          
        </Route>
      </Routes>
    </div>
  )
}
