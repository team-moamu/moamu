
import { Link, Route, Routes } from 'react-router'

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
      <nav className="flex flex-wrap gap-4">
        <Link to="/">홈</Link>
        <Link to="/challenge">도전하기</Link>
        <Link to="/museum">미술관</Link>
        <Link to="/collection">보관함</Link>
        <Link to="/mypage">마이</Link>
      </nav>

      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/challenge" element={<ChallengePage />} />
        <Route path="/museum" element={<MuseumPage />} />
        <Route path="/collection" element={<CollectionPage />} />
        <Route path="/mypage" element={<MyPage />} />
        <Route path="*" element={<h1>페이지를 찾을 수 없습니다.</h1>} />
      </Routes>
    </div>
  )
}
