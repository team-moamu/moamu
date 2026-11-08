import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { Link, MemoryRouter, Route, Routes, useLocation } from 'react-router'
import { ChallengeCompletionPage } from '../pages/challenge/ChallengeCompletionPage'
import type { ChallengeCompletionResponse } from '../types/Challenge'
import artworkUrl from './artwork-preview.svg'
import '../index.css'
import '../styles/font.css'

// 실제 API를 호출하지 않는 로컬 개발 전용 진입점
if (!import.meta.env.DEV || !['localhost', '127.0.0.1'].includes(location.hostname)) {
  throw new Error('목업은 로컬 개발 서버에서만 실행할 수 있습니다.')
}

const params = new URL(location.href).searchParams
const mode = params.get('case') ?? 'partial'
const duration = [7, 14, 30].includes(Number(params.get('duration'))) ? Number(params.get('duration')) : 14
const indices = Array.from({ length: duration }, (_, index) => index)
const revealed = mode === 'full' ? indices : mode === 'zero' ? []
  : indices.filter(index => index % 3 !== 1).slice(0, Math.floor(duration * .6))
const cols = Math.ceil(Math.sqrt(duration))
const endDate = new Date(Date.UTC(2026, 9, duration)).toISOString().slice(0, 10)
const fixture: ChallengeCompletionResponse = {
  challengeId: 5, challengeTitle: '명찰 챙기기', status: 'COMPLETED',
  startDate: '2026-10-01', endDate, durationDays: duration,
  artwork: { artworkId: 30, title: '연못과 다리 · 샘플', imageUrl: mode === 'no-image' ? null : artworkUrl },
  gridRows: Math.ceil(duration / cols), gridCols: cols,
  revealedPieceIndices: revealed, totalPieceCount: duration, earnedPieceCount: revealed.length,
  missingPieceCount: duration - revealed.length, allPiecesEarned: revealed.length === duration,
}
let requestCount = 0
const originalFetch = window.fetch.bind(window)
window.fetch = async (input, init) => {
  const target = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
  const path = new URL(target, location.origin).pathname
  if (!path.startsWith('/api/')) return originalFetch(input, init)
  if (init?.signal?.aborted) throw new DOMException('Aborted', 'AbortError')
  let status = 200
  let body: unknown = { success: true, data: fixture }
  if (path !== '/api/challenges/5/completion' || (init?.method && init.method !== 'GET')) {
    status = 404
    body = { success: false, error: { code: 'RESOURCE_NOT_FOUND', message: '목업에 없는 요청입니다.' } }
  } else if (mode === 'pending') {
    status = 409
    body = { success: false, error: { code: 'CHALLENGE_NOT_COMPLETED', message: '아직 도전 결과가 확정되지 않았습니다.' } }
  } else if (mode === 'error' && ++requestCount <= 2) {
    status = 503
    body = { success: false, error: { code: 'MOCK_ERROR', message: '샘플 조회 오류입니다. 다시 불러오기를 눌러 주세요.' } }
  }
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

export function CompletionMockDestination() {
  const location = useLocation()
  return (
    <main style={{ padding: 24 }}>
      <h1 className="text-heading">이동 경로 확인</h1>
      <p>{location.pathname}</p>
      <Link to="/challenge/5/completion">완료 화면 다시 보기</Link>
    </main>
  )
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MemoryRouter initialEntries={['/challenge/5/completion']}>
      <Routes>
        <Route path="/challenge/:challengeId/completion" element={<ChallengeCompletionPage />} />
        <Route path="*" element={<CompletionMockDestination />} />
      </Routes>
    </MemoryRouter>
  </StrictMode>,
)

