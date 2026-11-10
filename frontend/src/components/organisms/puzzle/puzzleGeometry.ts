export interface PuzzleLayout {
  rows: number
  cols: number
  cells: number[]
}

// 칸 번호와 획득 조각 번호를 분리한다. 같은 번호의 칸은 하나의 조각이다.
export function puzzleLayout(total: number, rows: number, cols: number): PuzzleLayout {
  if (total === 7) return { rows: 3, cols: 3, cells: [0, 0, 1, 2, 3, 4, 5, 6, 6] }
  if (total === 14) return {
    rows: 4, cols: 4,
    cells: [0, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 13],
  }
  if (total === 30) return { rows: 5, cols: 6, cells: Array.from({ length: 30 }, (_, i) => i) }
  return { rows, cols, cells: Array.from({ length: rows * cols }, (_, i) => i) }
}

// 현재 정책의 조각은 단일 칸 또는 가로로 인접한 두 칸의 직사각형이다.
// 외곽만 그리므로 합쳐진 두 칸 사이에는 선이나 돌기가 생기지 않는다.
export function puzzlePiecePath(index: number, layout: PuzzleLayout): string {
  const { rows, cols, cells } = layout
  const owned = cells.flatMap((piece, cell) => piece === index ? [cell] : [])
  if (owned.length === 0) return ''
  const top = Math.floor(Math.min(...owned) / cols)
  const bottom = Math.floor(Math.max(...owned) / cols) + 1
  const left = Math.min(...owned.map(cell => cell % cols))
  const right = Math.max(...owned.map(cell => cell % cols)) + 1
  const width = 1000 / cols
  const height = 1000 / rows
  const depth = Math.min(width, height) * 0.22
  const direction = (boundary: number) => boundary % 2 === 0 ? 1 : -1

  function edge(col: number, row: number, dc: number, dr: number, bump: number) {
    const sx = col * width
    const sy = row * height
    const dx = dc * width
    const dy = dr * height
    const length = Math.hypot(dx, dy)
    const point = (t: number, offset = 0) =>
      `${sx + dx * t - dy / length * offset},${sy + dy * t + dx / length * offset}`
    if (bump === 0) return `L ${point(1)}`
    const d = depth * bump
    return `L ${point(0.36)} C ${point(0.44)}, ${point(0.34, d)}, ${point(0.5, d)} C ${point(0.66, d)}, ${point(0.56)}, ${point(0.64)} L ${point(1)}`
  }

  const path = [`M ${left * width},${top * height}`]
  for (let col = left; col < right; col++) {
    path.push(edge(col, top, 1, 0, top === 0 ? 0 : direction(top + col)))
  }
  for (let row = top; row < bottom; row++) {
    path.push(edge(right, row, 0, 1, right === cols ? 0 : direction(row + right)))
  }
  for (let col = right - 1; col >= left; col--) {
    path.push(edge(col + 1, bottom, -1, 0, bottom === rows ? 0 : -direction(bottom + col)))
  }
  for (let row = bottom - 1; row >= top; row--) {
    path.push(edge(left, row + 1, 0, -1, left === 0 ? 0 : -direction(row + left)))
  }
  return [...path, 'Z'].join(' ')
}
