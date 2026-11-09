import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Link, MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { VerificationResultPage } from '../pages/challenge/VerificationResultPage'
import { VerificationPage } from '../pages/challenge/verificationPage'
import { MainLayout } from '../components/layout/MainLayout'
import { sampleChallenge, sampleSuccess } from './verification-result-fixtures'
import type { VerificationSubmitResponse } from '../types/Challenge'
import '../index.css'
import '../styles/font.css'

// 일반 앱에서는 import하지 않으며 배포·외부 호스트에서는 실행하지 않음
if (!import.meta.env.DEV || !['localhost', '127.0.0.1'].includes(location.hostname)) {
  throw new Error('목업은 로컬 개발 서버에서만 실행할 수 있습니다.')
}

const mode = new URL(location.href).searchParams.get('case') ?? 'success'
const status = mode === 'review' ? 'REVIEW_REQUIRED'
  : ['failed', 'exhausted'].includes(mode) ? 'FAILED' : 'SUCCESS'
let attempts = mode === 'exhausted' ? 3 : 1
let resultAttempts = 0
let retried = false
const submission: VerificationSubmitResponse = {
  verificationId: 101, mediaId: 201, aiTaskId: 301, status,
  checks: [
    { code: 'C1', status: 'MET', reason: '명찰이 보여요' },
    { code: 'C2', status: mode === 'review' ? 'UNCERTAIN' : status === 'FAILED' ? 'NOT_MET' : 'MET',
      reason: '명찰을 들고 있는 손이 보여요' },
  ],
}

function json(data: unknown, responseStatus = 200) {
  return new Response(JSON.stringify(data), { status: responseStatus, headers: { 'Content-Type': 'application/json' } })
}

const originalFetch = window.fetch.bind(window)
window.fetch = async (input, init) => {
  const target = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
  const url = new URL(target, location.origin)
  if (!url.pathname.startsWith('/api/')) return originalFetch(input, init)
  if (init?.signal?.aborted) throw new DOMException('Aborted', 'AbortError')

  // 모킹되지 않은 요청도 백엔드로 보내지 않음
  if (/^\/api\/verification\/\d+\/result$/.test(url.pathname)) {
    resultAttempts++
    // StrictMode의 초기 중복 조회에도 오류가 보이도록 클릭 후 조회부터 성공
    if (mode === 'error' && resultAttempts <= 2) {
      return json({ success: false, error: { code: 'MOCK_ERROR', message: '샘플 조회 오류입니다. 다시 불러오기를 눌러 주세요.' } }, 503)
    }
    const data = sampleSuccess(Number(url.pathname.split('/')[3]))
    if (mode === 'no-image') data.artwork.imageUrl = null
    if (mode === 'seven') {
      data.gridCols = 3
      data.openedPieceIndex = 4
      data.revealedPieceIndices = [0, 1, 4]
      data.totalPieceCount = 7
    }
    return json({ success: true, data })
  }
  if (url.pathname === '/api/verification') {
    return json({ success: true, data: { ...sampleChallenge, attemptCount: attempts } })
  }
  if (url.pathname === '/api/media' && init?.method === 'POST') {
    if (!retried) attempts++
    retried = true
    return json({ success: true, data: {
      ...submission, verificationId: 102, status: 'SUCCESS',
      checks: sampleSuccess().checks,
    } })
  }
  return json({ success: false, error: { code: 'MOCK_NOT_DEFINED', message: '목업에서 지원하지 않는 API입니다.' } }, 404)
}

export function MockDestination() {
  const location = useLocation()
  return (
    <main style={{ padding: '40px 24px' }}>
      <h1 className="text-heading">이동 경로 확인</h1>
      <p style={{ marginTop: 16 }}>{location.pathname}{location.search}</p>
      <p style={{ marginTop: 16 }}>목업에서는 실제 데이터를 조회하지 않아요.</p>
      <Link to="/challenge/5/verification/result/101" state={{ result: submission, challenge: sampleChallenge }}>
        결과 화면으로 돌아가기
      </Link>
    </main>
  )
}

// 실제 페이지를 MemoryRouter에 연결해 버튼 이동도 실제 경로 기준으로 확인
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MemoryRouter initialEntries={[{
      pathname: '/challenge/5/verification/result/101',
      state: { result: submission, challenge: sampleChallenge },
    }]}>
      <Routes>
        <Route path="/challenge/:challengeId/verification/result/:verificationId" element={<VerificationResultPage />} />
        <Route element={<MainLayout showBottomNav background="white" headerType="title" title="오늘의 인증" />}>
          <Route path="/challenge/:challengeId/verification" element={<VerificationPage />} />
        </Route>
        <Route path="*" element={<MockDestination />} />
      </Routes>
    </MemoryRouter>
  </StrictMode>,
)

