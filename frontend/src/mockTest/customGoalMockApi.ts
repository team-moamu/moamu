import type { VerifiableGoalAnalysis } from '../types/GoalAnalysis'

export type MockScenario = 'success' | 'unverifiable' | 'analysis-error' | 'create-error' | 'expired'
export type MockCreation = { challengeId: number; goal: string; startDate: string; durationDays: number }

// 이 모듈은 별도 목업 진입점에서만 불러온다. 실제 앱에서는 fetch를 대체하지 않는다.
export function installCustomGoalMock() {
  const originalFetch = globalThis.fetch
  const analyses = new Map<string, VerifiableGoalAnalysis>()
  const creations = new Map<string, MockCreation>()
  let scenario: MockScenario = 'success'
  let lastCreation: MockCreation | null = null
  let nextId = 1
  const respond = (data: unknown) => Response.json({ success: true, data })
  const fail = (code: string, message: string, status: number) => Response.json({
    success: false, error: { code, message },
  }, { status })

  globalThis.fetch = async (input, options = {}) => {
    const signal = options.signal
    signal?.throwIfAborted()
    await new Promise<void>((resolve, reject) => {
      const onAbort = () => { clearTimeout(timer); reject(signal?.reason) }
      const timer = setTimeout(() => {
        signal?.removeEventListener('abort', onAbort)
        resolve()
      }, 450)
      signal?.addEventListener('abort', onAbort, { once: true })
    })
    signal?.throwIfAborted()
    const url = typeof input === 'string' ? input : input instanceof URL ? input.pathname : input.url
    if (url === '/api/goalTemaplate' && (!options.method || options.method === 'GET')) {
      return respond([
        { goalTemplateId: 1, goalName: '물 마시기', description: '매일 물 한 잔 마시기' },
        { goalTemplateId: 2, goalName: '계단 운동하기', description: '매일 계단 오르기' },
      ])
    }
    const body = typeof options.body === 'string' ? JSON.parse(options.body) : {}
    if (url === '/api/goals/analyze' && options.method === 'POST') {
      if (scenario === 'analysis-error') return fail('AI_TECHNICAL_ERROR', '목표 분석에 실패했어요. 다시 시도해 주세요. (목업)', 502)
      if (typeof body.goal !== 'string' || !body.goal.trim() || body.goal.length > 500) {
        return fail('INVALID_REQUEST', '목표를 1~500자로 입력해 주세요.', 400)
      }
      if (scenario === 'unverifiable' || body.goal.includes('긍정적으로 생각하기')) {
        return respond({ status: 'NOT_VERIFIABLE', alternatives: [
          '매일 감사 일기 3줄 쓰기', '아침에 스트레칭하기', '자기 전 책 10쪽 읽기',
        ] })
      }
      const analysis: VerifiableGoalAnalysis = {
        status: 'VERIFIABLE', analysisId: crypto.randomUUID(), goal: body.goal,
        normalizedGoal: [...body.goal.trim()].slice(0, 50).join(''),
        conditions: ['목표를 실천한 모습이나 결과물이 사진에 보여야 해요.'],
        photoGuide: '실천한 내용이 잘 보이도록 촬영해 주세요.',
        verificationScope: '사진에 보이는 실천 흔적만 확인해요. 시간과 횟수는 판단하지 않아요.',
      }
      analyses.set(analysis.analysisId, analysis)
      return respond(analysis)
    }
    if (url === '/api/challenges/custom' && options.method === 'POST') {
      if (scenario === 'expired') {
        analyses.delete(body.analysisId)
        return fail('ANALYSIS_NOT_FOUND', '분석 결과가 만료되었어요. 목표를 다시 분석해 주세요. (목업)', 404)
      }
      if (scenario === 'create-error') return fail('INTERNAL_SERVER_ERROR', '도전 생성에 실패했어요. 다시 시도해 주세요. (목업)', 500)
      const analysis = analyses.get(body.analysisId)
      if (!analysis) return fail('ANALYSIS_NOT_FOUND', '목표를 다시 분석해 주세요.', 404)
      if (![7, 14, 30].includes(body.durationDays) || !/^\d{4}-\d{2}-\d{2}$/.test(body.startDate ?? '')) {
        return fail('INVALID_REQUEST', '시작일과 기간을 확인해 주세요.', 400)
      }
      const existing = creations.get(body.analysisId)
      if (existing && (existing.startDate !== body.startDate || existing.durationDays !== body.durationDays)) {
        return fail('ANALYSIS_ALREADY_CONFIRMED', '다른 날짜 또는 기간으로 이미 확정된 분석이에요.', 409)
      }
      lastCreation = existing ?? { challengeId: nextId++, goal: analysis.normalizedGoal,
        startDate: body.startDate, durationDays: body.durationDays }
      creations.set(body.analysisId, lastCreation)
      return respond({ challengeId: lastCreation.challengeId })
    }
    if (url === '/api/challenges' && options.method === 'POST') {
      lastCreation = { challengeId: nextId++, goal: body.goalTemplateId === 1 ? '물 마시기' : '계단 운동하기',
        startDate: body.startDate, durationDays: body.durationDays }
      return new Response(null, { status: 200 })
    }
    // 목업에서 처리하지 않는 요청도 실제 서버로 전달하지 않는다.
    return fail('MOCK_UNSUPPORTED', `목업에서 지원하지 않는 요청입니다: ${url}`, 404)
  }

  return {
    setScenario: (next: MockScenario) => { scenario = next },
    getLastCreation: () => lastCreation,
    restore: () => { globalThis.fetch = originalFetch },
  }
}
