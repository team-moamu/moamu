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
  const { puzzleLayout, puzzlePiecePath } = await server.ssrLoadModule('/src/components/organisms/puzzle/puzzleGeometry.ts')
  const { getChallengeCompletion, ChallengeCompletionError } = await server.ssrLoadModule('/src/api/ChallengeApi.ts')

  for (const total of [7, 14, 30]) {
    const cols = Math.ceil(Math.sqrt(total))
    for (const earned of [0, Math.floor(total / 2), total]) {
      const revealed = Array.from({ length: total }, (_, index) => index).filter(index => index % 2 === 0).slice(0, earned)
      const indices = earned === total ? Array.from({ length: total }, (_, index) => index) : revealed
      const html = renderToStaticMarkup(React.createElement(ArtworkPuzzle, {
        imageUrl: 'https://example.com/signed-artwork', title: 'Painting',
        gridRows: Math.ceil(total / cols), gridCols: cols, totalPieceCount: total,
        revealedPieceIndices: indices, allPiecesEarned: earned === total,
      }))
      assert.equal((html.match(/data-piece-index=/g) ?? []).length, earned === total ? 0 : total)
      assert.ok(html.includes('aspect-square'))
      assert.equal((html.match(/<img /g) ?? []).length, 1)
      assert.equal((html.match(/data-revealed="true"/g) ?? []).length, earned === total ? 0 : indices.length)
      assert.ok(!html.includes('data-new="true"'))
      assert.ok(html.includes('https://example.com/signed-artwork'))
      if (earned !== total) {
        assert.ok(html.includes('<svg'))
        assert.equal((html.match(/fill="#000000"/g) ?? []).length, total - indices.length)
        assert.ok(html.includes(' C '), 'Pieces have curved interlocking edges')
      }
      passed++
    }
  }

  for (const [total, rows, cols] of [[7, 3, 3], [14, 4, 4], [30, 5, 6]]) {
    // 과거 개발 데이터의 1×N 값도 정책에 맞춰 표시한다.
    const layout = puzzleLayout(total, 1, total)
    assert.equal(layout.rows, rows)
    assert.equal(layout.cols, cols)
    assert.equal(layout.cells.length, rows * cols)
    assert.deepEqual([...new Set(layout.cells)], Array.from({ length: total }, (_, i) => i))
    const doubled = []
    for (let piece = 0; piece < total; piece++) {
      const cells = layout.cells.flatMap((value, cell) => value === piece ? [cell] : [])
      assert.ok(cells.length === 1 || cells.length === 2)
      if (cells.length === 2) {
        doubled.push(piece)
        assert.equal(cells[1], cells[0] + 1)
        assert.equal(Math.floor(cells[0] / cols), Math.floor(cells[1] / cols))
      }
      const path = puzzlePiecePath(piece, layout)
      assert.equal((path.match(/M /g) ?? []).length, 1, 'Merged piece has one outer contour')
      const curvedEdges = (path.match(/C /g) ?? []).length / 2
      assert.equal((path.match(/L /g) ?? []).length - curvedEdges, cells.length === 2 ? 6 : 4, 'No internal edge in merged piece')
    }
    assert.deepEqual(doubled, total === 30 ? [] : [0, total - 1])
    const html = renderToStaticMarkup(React.createElement(ArtworkPuzzle, {
      imageUrl: 'https://example.com/artwork', title: 'Painting',
      gridRows: 1, gridCols: total, totalPieceCount: total,
      revealedPieceIndices: [0, total - 1], highlightedPieceIndex: 0,
    }))
    assert.equal((html.match(/data-revealed="true"/g) ?? []).length, 2)
    assert.ok(html.includes(`d="${puzzlePiecePath(0, layout)}" data-piece-index="0" data-revealed="true"`))
    assert.ok(html.includes('viewBox="0 0 1000 1000"'))
    passed++
  }
  assert.deepEqual(puzzleLayout(7, 3, 3).cells, [0, 0, 1, 2, 3, 4, 5, 6, 6])

  const daily = renderToStaticMarkup(React.createElement(ArtworkPuzzle, {
    imageUrl: null, title: null, gridRows: 3, gridCols: 3, totalPieceCount: 7,
    revealedPieceIndices: [0, 4], highlightedPieceIndex: 4,
  }))
  assert.equal((daily.match(/data-new="true"/g) ?? []).length, 1)
  assert.ok(daily.includes('data-image-placeholder'))
  passed++

  const finalDaily = renderToStaticMarkup(React.createElement(ArtworkPuzzle, {
    imageUrl: 'https://example.com/artwork', title: 'Painting',
    gridRows: 3, gridCols: 3, totalPieceCount: 7,
    revealedPieceIndices: [0, 1, 2, 3, 4, 5, 6], highlightedPieceIndex: 6,
  }))
  assert.ok(!finalDaily.includes('<svg'), 'Final daily verification reveals the entire artwork')
  passed++

  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/challenges/5/completion')
    assert.equal(new Headers(options.headers).get('Authorization'), 'Bearer test-token')
    assert.ok(!options.method || options.method === 'GET')
    return Response.json({ success: true, data: { status: 'COMPLETED', challengeId: 5 } })
  }
  assert.equal((await getChallengeCompletion(5, undefined, 'test-token')).challengeId, 5)
  passed++

  for (const code of ['CHALLENGE_NOT_COMPLETED', 'RESOURCE_NOT_FOUND', 'AUTHENTICATION_REQUIRED']) {
    globalThis.fetch = async () => Response.json({ success: false, error: { code, message: code } }, { status: 409 })
    await assert.rejects(() => getChallengeCompletion(5, undefined, 'test-token'), error => error instanceof ChallengeCompletionError && error.code === code)
    passed++
  }
  for (const data of [{ status: 'IN_PROGRESS', challengeId: 5 }, { status: 'COMPLETED', challengeId: 99 }]) {
    globalThis.fetch = async () => Response.json({ success: true, data })
    await assert.rejects(() => getChallengeCompletion(5, undefined, 'test-token'), ChallengeCompletionError)
    passed++
  }
  console.log(`Passed ${passed} completion API / puzzle checks.`)
} finally {
  globalThis.fetch = originalFetch
  await server.close()
}
