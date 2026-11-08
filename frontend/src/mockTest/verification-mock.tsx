// 임시 로컬 테스트 진입점. 확인 후 이 파일과 verification-mock.html만 삭제
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { MemoryRouter, Route, Routes } from 'react-router'
import { MainLayout } from '../components/layout/MainLayout'
import { VerificationPage } from '../pages/challenge/verificationPage'
import { VerificationResultPage } from '../pages/challenge/VerificationResultPage'
import { sampleSuccess } from './verification-result-fixtures'
import '../index.css'

// 일반 앱의 진입점에서는 불러오지 않으며 로컬 개발 서버에서만 실행
if (!import.meta.env.DEV || !['localhost', '127.0.0.1'].includes(location.hostname)) {
  throw new Error('모킹 테스트는 로컬 개발 서버에서만 실행할 수 있습니다.')
}

const mode = new URL(location.href).searchParams.get('case') ?? 'success'
const selector = document.getElementById('mock-case') as HTMLSelectElement
selector.value = mode
selector.addEventListener('change', () => {
  const url = new URL(location.href)
  url.searchParams.set('case', selector.value)
  location.assign(url)
})

let attempts = 0
let getCount = 0
let postCount = 0
let previousKey: string | null = null
const completed = new Map<string, unknown>()
const pending = new Set<string>()
const originalFetch = window.fetch.bind(window)

function showMetrics(message = '') {
  document.getElementById('mock-metrics')!.textContent =
    `도전 조회 ${getCount}회 · 인증 제출 ${postCount}회 ${message}`
}

function json(status: number, body: unknown) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

function error(status: number, code: string, message: string) {
  return json(status, { success: false, error: { code, message } })
}

// 실제 API 함수는 그대로 실행하고 fetch 응답만 대체. API 요청은 서버로 보내지 않음
window.fetch = async (input, init) => {
  const target = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
  const url = new URL(target, location.origin)
  if (!url.pathname.startsWith('/api/')) return originalFetch(input, init)

  if (/^\/api\/verification\/\d+\/result$/.test(url.pathname)) {
    return json(200, { success: true, data: sampleSuccess(Number(url.pathname.split('/')[3])) })
  }

  // 로그인 없이 인증 페이지를 표시하고 제출 후 최신 시도 횟수 반환
  if (url.pathname === '/api/verification' && (!init?.method || init.method === 'GET')) {
    getCount++
    showMetrics()
    return json(200, { success: true, data: {
      challengeId: 5,
      challengeTitle: '명찰 챙기기 · 모킹 테스트',
      scheduleId: 36,
      dayNo: 3,
      dayCount: 14,
      attemptCount: attempts,
      description: ['명찰이 보여요', '명찰을 들고 있는 손이 보여요'],
    } })
  }

  // 사진 선택만으로는 실행되지 않고 인증하기 버튼을 누를 때만 처리
  if (url.pathname === '/api/media' && init?.method === 'POST') {
    postCount++
    const submissionNumber = postCount
    const form = init.body
    const key = new Headers(init.headers).get('Idempotency-Key')
    if (!(form instanceof FormData) || !(form.get('image') instanceof File)
        || form.get('scheduleId') !== '36' || !key) {
      return error(400, 'INVALID_REQUEST', '사진, 회차 ID 또는 멱등성 키가 누락되었습니다.')
    }
    if (completed.has(key)) return json(200, completed.get(key))
    if (pending.has(key)) return error(409, 'CONCURRENT_MODIFICATION', '처리 중인 요청입니다.')

    const keyMessage = previousKey === null ? '요청 형식 정상'
      : previousKey === key ? '동일 키 재전송' : '새 멱등성 키 사용'
    previousKey = key
    pending.add(key)
    showMetrics(`${keyMessage} · AI 판정 대기`)

    // AI 응답 지연을 가정하여 기존 로딩 화면 확인
    const signal = init.signal
    try {
      await new Promise<void>((resolve, reject) => {
        if (signal?.aborted) return reject(new DOMException('Aborted', 'AbortError'))
        const abort = () => {
          clearTimeout(timer)
          reject(new DOMException('Aborted', 'AbortError'))
        }
        const timer = setTimeout(() => {
          signal?.removeEventListener('abort', abort)
          resolve()
        }, 3000)
        signal?.addEventListener('abort', abort, { once: true })
      })

      // 첫 요청은 기술 오류, 재시도는 성공. 기술 오류는 시도 횟수를 늘리지 않음
      if (mode === 'technical' && submissionNumber === 1) {
        return error(502, 'AI_TECHNICAL_ERROR', '모킹 테스트: AI 서버 오류입니다. 다시 인증해 주세요.')
      }

      attempts++
      const status = mode === 'failed' ? 'FAILED' : mode === 'review' ? 'REVIEW_REQUIRED' : 'SUCCESS'
      const checkStatus = mode === 'failed' ? 'NOT_MET' : mode === 'review' ? 'UNCERTAIN' : 'MET'
      const reason = mode === 'failed' ? '명찰을 확인할 수 없습니다.'
        : mode === 'review' ? '명찰이 흐릿해서 추가 검토가 필요합니다.' : '명찰이 선명하게 보입니다.'
      const body = { success: true, data: {
        verificationId: 100 + attempts,
        mediaId: 200 + attempts,
        aiTaskId: 300 + attempts,
        status,
        checks: [
          { code: 'C1', status: checkStatus, reason },
          { code: 'C2', status: 'MET', reason: '명찰을 들고 있는 손이 보입니다.' },
        ],
      } }
      completed.set(key, body)
      return json(200, body)
    } finally {
      pending.delete(key)
    }
  }

  // 정의하지 않은 API도 실제 백엔드로 전달하지 않음
  return error(404, 'MOCK_NOT_DEFINED', `모킹되지 않은 요청입니다: ${url.pathname}`)
}

showMetrics()

// 실제 인증 페이지를 재사용. 새로고침해도 테스트 HTML 주소를 유지
createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <MemoryRouter initialEntries={['/challenge/5/verification?scheduleId=36']}>
      <Routes>
        <Route path="/challenge/:challengeId/verification/result/:verificationId" element={<VerificationResultPage />} />
        <Route element={<MainLayout showBottomNav background="white" headerType="title" title="오늘의 인증" />}>
          <Route path="/challenge/:challengeId/verification" element={<VerificationPage />} />
        </Route>
      </Routes>
    </MemoryRouter>
  </StrictMode>,
)
