import { StrictMode, useState } from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router'
import { MainLayout } from '../components/layout/MainLayout'
import { SuccessButton } from '../components/atoms/button/successButton'
import { ChallengeCreatePage } from '../pages/challenge/ChallengeCreatePage'
import { GoalVerificationConditionsPage } from '../pages/challenge/GoalVerificationConditionsPage'
import { GoalVerificationUnavailablePage } from '../pages/challenge/GoalVerificationUnavailablePage'
import { installCustomGoalMock, type MockScenario } from './customGoalMockApi'
import '../styles/font.css'
import '../index.css'

if (!import.meta.env.DEV) throw new Error('목업은 개발 모드에서만 실행할 수 있습니다.')
const mock = installCustomGoalMock()

export function MockControls() {
  const [scenario, setScenario] = useState<MockScenario>('success')
  return (
    <aside className="mx-auto w-full max-w-[430px] space-y-2 bg-[#FFF8ED] p-4 text-sm">
      <p className="m-0 font-semibold">프론트 전용 목업 · 서버 요청 및 실제 저장 없음</p>
      <label className="flex items-center gap-2">
        다음 요청 시나리오
        <select className="min-w-0 rounded border bg-white p-2" value={scenario} onChange={(event) => {
          const next = event.target.value as MockScenario
          setScenario(next)
          mock.setScenario(next)
        }}>
          <option value="success">성공</option>
          <option value="unverifiable">인증 불가</option>
          <option value="analysis-error">분석 오류</option>
          <option value="create-error">생성 오류</option>
          <option value="expired">분석 만료</option>
        </select>
      </label>
      <p className="m-0">대안 목표를 재분석하거나 오류 후 재시도하려면 성공으로 바꿔 주세요.</p>
    </aside>
  )
}

export function MockCreatedPage() {
  const navigate = useNavigate()
  const creation = mock.getLastCreation()
  return (
    <div className="mx-auto flex w-full max-w-[430px] flex-col gap-5 px-5 py-6">
      <h1 className="text-xl font-semibold">도전 생성 완료 (목업)</h1>
      {creation && <dl className="space-y-2">
        <dt>도전 번호</dt><dd>{creation.challengeId}</dd>
        <dt>목표</dt><dd>{creation.goal}</dd>
        <dt>시작일</dt><dd>{creation.startDate}</dd>
        <dt>기간</dt><dd>{creation.durationDays}일</dd>
      </dl>}
      <p>화면 흐름 확인용 결과입니다. 새로고침하면 목업 데이터가 초기화됩니다.</p>
      <SuccessButton onClick={() => navigate('/challenge/create', { replace: true })}>다시 만들기</SuccessButton>
    </div>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MemoryRouter initialEntries={['/challenge/create']}>
      <MockControls />
      <Routes>
        <Route element={<MainLayout headerType="back" title="도전 생성" />}>
          <Route path="/challenge/create" element={<ChallengeCreatePage />} />
        </Route>
        <Route element={<MainLayout headerType="back" title="인증 조건 확인" background="white" />}>
          <Route path="/challenge/create/conditions" element={<GoalVerificationConditionsPage />} />
          <Route path="/challenge/create/conditions/unverifiable" element={<GoalVerificationUnavailablePage />} />
        </Route>
        <Route path="/" element={<MockCreatedPage />} />
      </Routes>
    </MemoryRouter>
  </StrictMode>,
)
