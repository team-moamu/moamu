import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { WalkingMu } from '../../components/molecules/gallery/WalkingMu'
import background from '../../assets/images/gallery/gallery.png'
import './GalleryDesktopPage.css'

export function GalleryDesktopPage() {
  const [showGuide, setShowGuide] = useState(true)

  useEffect(() => {
    const timer = window.setTimeout(() => setShowGuide(false), 3000)
    return () => window.clearTimeout(timer)
  }, [])

  const screenRef = useRef<HTMLElement>(null)
  const viewportRef = useRef<HTMLDivElement>(null)
  const worldRef = useRef<HTMLDivElement>(null)
  const muRef = useRef<HTMLDivElement>(null)
  const [pose, setPose] = useState({ direction: 'right' as 'left' | 'right', frame: 0 })

  // 첫 화면을 그리기 전에 뮤의 크기와 위치를 확정한다.
  useLayoutEffect(() => {
    const screen = screenRef.current
    const viewport = viewportRef.current
    const world = worldRef.current
    const mu = muRef.current
    if (!screen || !viewport || !world || !mu) return
    const keys = new Set<string>()
    let width = 0
    let worldWidth = 0
    let spriteSize = 0
    let edgePadding = 0
    let position = 0
    let initialized = false
    let previousTime = 0
    let walkingTime = 0
    let direction: 'left' | 'right' = 'right'
    let lastPose = ''
    let animation = 0
    const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)')

    // 캐릭터 전체와 여백이 전시 공간 안에 남도록 중심 좌표를 제한한다.
    function clampPosition(value: number) {
      return Math.max(spriteSize / 2 + edgePadding, Math.min(width - spriteSize / 2 - edgePadding, value))
    }
    function renderPosition() {
      // 뮤가 1px 이동하면 배경도 반대 방향으로 1px 이동한다.
      const camera = (worldWidth - width) / 2 + (position - width / 2)
      world!.style.transform = `translateX(${-camera}px)`
      mu!.style.transform = `translateX(${position - spriteSize / 2}px)`
    }
    function resize() {
      const relativePosition = width > 0 ? position / width : 0
      const visible = window.visualViewport
      const availableWidth = visible?.width ?? window.innerWidth
      const availableHeight = visible?.height ?? window.innerHeight
      screen!.style.width = `${availableWidth}px`
      screen!.style.height = `${availableHeight}px`
      screen!.style.left = `${visible?.offsetLeft ?? 0}px`
      screen!.style.top = `${visible?.offsetTop ?? 0}px`
      // 가로와 세로 중 더 제한적인 쪽에 맞춰 배경 전체를 축소한다.
      const scale = Math.min(availableWidth / 3072, availableHeight / 1024)
      width = 3072 * scale
      const height = 1024 * scale
      viewport!.style.width = `${width}px`
      viewport!.style.height = `${height}px`
      // 배경 한 장을 화면에 맞추고, 옆 공간을 이어 붙여 좌우 이동을 유지한다.
      worldWidth = width * 3
      edgePadding = Math.min(16, width * 0.03, height * 0.03)
      spriteSize = Math.min(Math.max(40, Math.min(160, height * 0.18, width * 0.22)), height * 0.3, width - edgePadding * 2)
      world!.style.width = `${worldWidth}px`
      mu!.style.width = `${spriteSize}px`
      mu!.style.top = `${Math.max(edgePadding, Math.min(height * 0.64, height - spriteSize - edgePadding))}px`
      position = clampPosition(initialized ? relativePosition * width : width / 2)
      initialized = true
      renderPosition()
    }
    function clearKeys() { keys.clear(); previousTime = 0 }
    function keyDown(event: KeyboardEvent) {
      if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return
      if (event.target instanceof HTMLElement && event.target.closest('input, textarea, select, [contenteditable="true"]')) return
      event.preventDefault()
      keys.add(event.key)
    }
    function keyUp(event: KeyboardEvent) { keys.delete(event.key) }
    function tick(time: number) {
      const delta = previousTime ? Math.min((time - previousTime) / 1000, 0.05) : 0
      previousTime = time
      const axis = Number(keys.has('ArrowRight')) - Number(keys.has('ArrowLeft'))
      const next = clampPosition(position + axis * 140 * delta)
      const moving = next !== position
      if (axis) direction = axis > 0 ? 'right' : 'left'
      walkingTime = moving ? walkingTime + delta : 0
      position = next
      renderPosition()
      const frame = moving && !reducedMotion.matches ? Math.floor(walkingTime / 0.12) % 6 : 0
      const signature = `${direction}-${frame}`
      if (signature !== lastPose) { setPose({ direction, frame }); lastPose = signature }
      animation = requestAnimationFrame(tick)
    }
    resize()
    window.addEventListener('resize', resize)
    window.visualViewport?.addEventListener('resize', resize)
    window.visualViewport?.addEventListener('scroll', resize)
    window.addEventListener('keydown', keyDown)
    window.addEventListener('keyup', keyUp)
    window.addEventListener('blur', clearKeys)
    document.addEventListener('visibilitychange', clearKeys)
    animation = requestAnimationFrame(tick)
    return () => {
      cancelAnimationFrame(animation)
      window.removeEventListener('resize', resize)
      window.visualViewport?.removeEventListener('resize', resize)
      window.visualViewport?.removeEventListener('scroll', resize)
      window.removeEventListener('keydown', keyDown)
      window.removeEventListener('keyup', keyUp)
      window.removeEventListener('blur', clearKeys)
      document.removeEventListener('visibilitychange', clearKeys)
    }
  }, [])

  return (
    <main ref={screenRef} className="desktop-gallery">
      <div ref={viewportRef} className="desktop-gallery-viewport" tabIndex={0}
        aria-label="뮤 이동 테스트. 왼쪽과 오른쪽 방향키로 이동하세요.">
        <div ref={worldRef} className="desktop-gallery-world">
          {[0, 1, 2].map(index => (
            <img key={index} src={background} className="desktop-gallery-background"
              style={{ left: `${index * 100 / 3}%` }}
              alt={index === 0 ? '모아뮤 미술관' : ''} draggable={false} />
          ))}
        </div>
        <div ref={muRef} className="desktop-gallery-character">
          <WalkingMu direction={pose.direction} frame={pose.frame} />
        </div>
        <p className={`desktop-gallery-guide${showGuide ? '' : ' desktop-gallery-guide--hidden'}`}
          role="status" aria-hidden={!showGuide}>← → 방향키로 뮤를 움직여 보세요</p>
      </div>
      <header className="desktop-gallery-header">
        <Link to="/museum" className="desktop-gallery-return">앱 화면으로 돌아가기</Link>
      </header>
    </main>
  )
}
