import assert from 'node:assert/strict'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { MemoryRouter } from 'react-router'
import { createServer } from 'vite'

const server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
const originalFetch = globalThis.fetch
const originalStorage = globalThis.sessionStorage
globalThis.sessionStorage = { getItem: () => null }
try {
  const { analyzeGoal } = await server.ssrLoadModule('/src/api/GoalAnalysisApi.ts')
  const { createCustomChallenge } = await server.ssrLoadModule('/src/api/ChallengeApi.ts')
  const analysis = {
    status: 'VERIFIABLE', analysisId: 'c167f881-5227-4133-8b89-025199213f2c',
    goal: '매일 감사 일기 쓰기', normalizedGoal: '감사 일기 쓰기',
    conditions: ['작성한 일기가 보여야 해요.'], photoGuide: '일기를 촬영해 주세요.',
    verificationScope: '사진에 보이는 기록만 확인해요.',
  }
  const respond = (data, status = 200) => new Response(JSON.stringify(data), {
    status, headers: { 'Content-Type': 'application/json' },
  })
  let requests = []
  globalThis.fetch = async (url, options) => {
    requests.push({ url, options })
    return respond({ success: true, data: analysis })
  }
  const controller = new AbortController()
  assert.deepEqual(await analyzeGoal(analysis.goal, controller.signal), analysis)
  assert.equal(requests[0].url, '/api/goals/analyze')
  assert.equal(requests[0].options.method, 'POST')
  assert.equal(requests[0].options.signal, controller.signal)
  assert.deepEqual(JSON.parse(requests[0].options.body), { goal: analysis.goal })

  const alternatives = { status: 'NOT_VERIFIABLE', alternatives: ['일기 쓰기', '책 읽기', '산책하기'] }
  globalThis.fetch = async () => respond({ success: true, data: alternatives })
  assert.deepEqual(await analyzeGoal('긍정적으로 생각하기'), alternatives)
  globalThis.fetch = async () => respond({ success: true, data: { ...analysis, analysisId: undefined } })
  await assert.rejects(() => analyzeGoal(analysis.goal), /응답/)
  globalThis.fetch = async () => respond({ success: false, error: { code: 'AI_TECHNICAL_ERROR', message: '분석 실패' } }, 502)
  await assert.rejects(() => analyzeGoal(analysis.goal), /분석 실패/)

  const request = { analysisId: analysis.analysisId, startDate: '2026-12-01', durationDays: 7 }
  globalThis.fetch = async (url, options) => {
    requests.push({ url, options })
    return respond({ success: true, data: { challengeId: 123 } })
  }
  assert.deepEqual(await createCustomChallenge(request), { challengeId: 123 })
  assert.equal(requests[1].url, '/api/challenges/custom')
  assert.deepEqual(JSON.parse(requests[1].options.body), request)
  globalThis.fetch = async () => respond({ success: false, error: { code: 'ANALYSIS_NOT_FOUND', message: '목표를 다시 분석해 주세요.' } }, 404)
  await assert.rejects(() => createCustomChallenge(request), (error) => error.code === 'ANALYSIS_NOT_FOUND')

  const { ChallengeCreatePage } = await server.ssrLoadModule('/src/pages/challenge/ChallengeCreatePage.tsx')
  const renderCreate = (state) => renderToStaticMarkup(React.createElement(MemoryRouter, {
    initialEntries: [{ pathname: '/challenge/create', state }],
  }, React.createElement(ChallengeCreatePage)))
  const accepted = renderCreate({ acceptedVerification: analysis, customGoal: analysis.goal, ...request })
  assert.match(accepted, /인증 조건을 수락했어요/)
  assert.doesNotMatch(accepted, /<button[^>]*disabled=""[^>]*>도전 생성하기/)
  const unaccepted = renderCreate({ customGoal: analysis.goal, ...request })
  assert.match(unaccepted, /<button[^>]*disabled=""[^>]*>도전 생성하기/)
  // 실제 API 함수와 프론트 목업을 함께 실행해 서버 없이 분석 → 확정을 검증한다.
  const { installCustomGoalMock } = await server.ssrLoadModule('/src/mockTest/customGoalMockApi.ts')
  globalThis.fetch = () => { throw new Error('Real network must never be used') }
  const mock = installCustomGoalMock()
  try {
    const { getGoalTemplates } = await server.ssrLoadModule('/src/api/GoalTemplateApi.ts')
    assert.equal((await getGoalTemplates()).length, 2)
    const result = await analyzeGoal('매일 감사 일기 쓰기')
    assert.equal(result.status, 'VERIFIABLE')
    const confirm = { ...request, analysisId: result.analysisId }
    const created = await createCustomChallenge(confirm)
    assert.equal(created.challengeId, 1)
    assert.deepEqual(await createCustomChallenge(confirm), created)
    assert.equal(mock.getLastCreation().goal, result.normalizedGoal)
    await assert.rejects(() => createCustomChallenge({ ...confirm, durationDays: 14 }),
      (error) => error.code === 'ANALYSIS_ALREADY_CONFIRMED')
    mock.setScenario('unverifiable')
    assert.equal((await analyzeGoal('다른 목표')).status, 'NOT_VERIFIABLE')
    mock.setScenario('analysis-error')
    await assert.rejects(() => analyzeGoal('매일 일기 쓰기'), /분석에 실패/)
    mock.setScenario('create-error')
    await assert.rejects(() => createCustomChallenge(confirm), /생성에 실패/)
    mock.setScenario('expired')
    await assert.rejects(() => createCustomChallenge(confirm), (error) => error.code === 'ANALYSIS_NOT_FOUND')
    mock.setScenario('success')
    const abort = new AbortController()
    const pending = analyzeGoal('매일 일기 쓰기', abort.signal)
    abort.abort()
    await assert.rejects(() => pending, (error) => error.name === 'AbortError')
    assert.equal((await fetch('/api/unsupported')).status, 404)
  } finally {
    mock.restore()
  }
  console.log('Custom goal API contracts, acceptance gate and frontend mock scenarios passed.')
} finally {
  globalThis.fetch = originalFetch
  if (originalStorage === undefined) delete globalThis.sessionStorage
  else globalThis.sessionStorage = originalStorage
  await server.close()
}
