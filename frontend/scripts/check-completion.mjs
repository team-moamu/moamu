// 별도 테스트 라이브러리 없이 API 처리와 공통 퍼즐의 HTML 출력을 검증
// 실행: node scripts/check-completion.mjs
import assert from 'node:assert/strict'
import React from 'react'
import { renderToStaticMarkup } from 'react-dom/server'
import { createServer } from 'vite'

const server = await createServer({ server: { middlewareMode: true }, appType: 'custom' })
const originalFetch = globalThis.fetch
let passed = 0
try {
  const { ArtworkPuzzle } = await server.ssrLoadModule('/src/components/organisms/puzzle/ArtworkPuzzle.tsx')
  const { getChallengeCompletion, ChallengeCompletionError } = await server.ssrLoadModule('/src/api/ChallengeApi.ts')

  for (const total of [7, 14, 30]) {
    const cols = Math.ceil(Math.sqrt(total))
    for (const earned of [0, Math.floor(total / 2), total]) {
      const revealed = Array.from({ length: total }, (_, index) => index).filter(index => index % 2 === 0).slice(0, earned)
      const indices = earned === total ? Array.from({ length: total }, (_, index) => index) : revealed
      const html = renderToStaticMarkup(React.createElement(ArtworkPuzzle, {
        imageUrl: 'https://example.com/signed-artwork', title: 'Painting',
        gridRows: Math.ceil(total / cols), gridCols: cols, totalPieceCount: total,
        revealedPieceIndices: indices, allPiecesEarned: earned === total, maskColor: 'black',
      }))
      assert.equal((html.match(/data-piece-index=/g) ?? []).length, earned === total ? 0 : Math.ceil(total / cols) * cols)
      assert.equal((html.match(/data-revealed="true"/g) ?? []).length, earned === total ? 0 : indices.length)
      assert.ok(!html.includes('data-new="true"'))
      assert.ok(html.includes('https://example.com/signed-artwork'))
      passed++
    }
  }

  const daily = renderToStaticMarkup(React.createElement(ArtworkPuzzle, {
    imageUrl: null, title: null, gridRows: 3, gridCols: 3, totalPieceCount: 7,
    revealedPieceIndices: [0, 4], highlightedPieceIndex: 4,
  }))
  assert.equal((daily.match(/data-new="true"/g) ?? []).length, 1)
  assert.ok(daily.includes('data-image-placeholder'))
  passed++

  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/challenges/5/completion')
    assert.equal(options.headers.Authorization, 'Bearer test-token')
    assert.ok(!options.method || options.method === 'GET')
    return Response.json({ success: true, data: { status: 'COMPLETED', challengeId: 5 } })
  }
  assert.equal((await getChallengeCompletion(5, undefined, 'test-token')).challengeId, 5)
  passed++

  for (const code of ['CHALLENGE_NOT_COMPLETED', 'RESOURCE_NOT_FOUND', 'AUTHENTICATION_REQUIRED']) {
    globalThis.fetch = async () => Response.json({ success: false, error: { code, message: code } }, { status: 409 })
    await assert.rejects(() => getChallengeCompletion(5), error => error instanceof ChallengeCompletionError && error.code === code)
    passed++
  }
  for (const data of [{ status: 'IN_PROGRESS', challengeId: 5 }, { status: 'COMPLETED', challengeId: 99 }]) {
    globalThis.fetch = async () => Response.json({ success: true, data })
    await assert.rejects(() => getChallengeCompletion(5), ChallengeCompletionError)
    passed++
  }
  console.log(`Passed ${passed} completion API / puzzle checks.`)
} finally {
  globalThis.fetch = originalFetch
  await server.close()
}
