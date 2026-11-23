import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { Link } from 'react-router'
import { WalkingMu } from '../../components/molecules/gallery/WalkingMu'
import { getGalleries } from '../../api/GalleryApi'
import type { GalleryResponse } from '../../types/Gallery'
import { GalleryEasel } from '../../components/molecules/gallery/GalleryEasel'
import background from '../../assets/images/gallery/gallery.png'
import './GalleryDesktopPage.css'

const ARTWORKS_PER_ROOM = 10

export function GalleryDesktopPage() {
  const [artworks, setArtworks] = useState<GalleryResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const [retry, setRetry] = useState(0)
  const [roomIndex, setRoomIndex] = useState(0)
  const roomCount = Math.max(1, Math.ceil(artworks.length / ARTWORKS_PER_ROOM))
  const currentRoom = Math.min(roomIndex, roomCount - 1)
  const roomArtworks = artworks.slice(currentRoom * ARTWORKS_PER_ROOM, (currentRoom + 1) * ARTWORKS_PER_ROOM)
  // 현재 전시실의 작품만 한 줄에 균등하게 배치한다.
  const exhibitCount = Math.max(1, roomArtworks.length)
  // 기본 크기는 최초 이젤의 ¼이며 양쪽 화면 여백을 유지한다.
  const easelWidth = Math.min(3.525, 72 / exhibitCount)

  useEffect(() => {
    const controller = new AbortController()
    getGalleries('endDate', 'desc', controller.signal).then(data => {
      if (!controller.signal.aborted) { setArtworks(data); setRoomIndex(0) }
    }).catch(() => {
      if (!controller.signal.aborted) setError(true)
    }).finally(() => {
      if (!controller.signal.aborted) setLoading(false)
    })
    return () => controller.abort()
  }, [retry])

  const [showGuide, setShowGuide] = useState(true)

  useEffect(() => {
    const timer = window.setTimeout(() => setShowGuide(false), 3000)
    return () => window.clearTimeout(timer)
  }, [])

  const screenRef = useRef<HTMLElement>(null)
  const viewportRef = useRef<HTMLDivElement>(null)
  const muRef = useRef<HTMLDivElement>(null)
  const [muPositionPercent, setMuPositionPercent] = useState(50)
  const [pose, setPose] = useState({ direction: 'right' as 'left' | 'right', frame: 0 })

  // 첫 화면을 그리기 전에 뮤의 크기와 위치를 확정한다.
  useLayoutEffect(() => {
    const screen = screenRef.current
    const viewport = viewportRef.current
    const mu = muRef.current
    if (!screen || !viewport || !mu) return
    const keys = new Set<string>()
    let width = 0
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
      mu!.style.transform = `translateX(${position - spriteSize / 2}px)`
      if (width > 0) setMuPositionPercent(position / width * 100)
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
      // 배경 위아래의 버튼 공간을 확보한 뒤 원본 비율로 맞춘다.
      const scale = Math.min(availableWidth / 3072, Math.max(1, availableHeight - 96) / 1024)
      width = 3072 * scale
      const height = 1024 * scale
      screen!.style.setProperty('--gallery-scene-width', `${width}px`)
      screen!.style.setProperty('--gallery-scene-height', `${height}px`)
      viewport!.style.width = `${width}px`
      viewport!.style.height = `${height}px`
      edgePadding = Math.min(16, width * 0.03, height * 0.03)
      spriteSize = Math.min(Math.max(40, Math.min(160, height * 0.18, width * 0.22)), height * 0.3, width - edgePadding * 2)
      mu!.style.width = `${spriteSize}px`
      mu!.style.top = `${Math.max(edgePadding, Math.min(height * 0.62, height - spriteSize - edgePadding))}px`
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
      const next = clampPosition(position + axis * 147 * delta)
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
        <img src={background} className="desktop-gallery-background"
          alt="모아뮤 미술관" draggable={false} />
        {/* 이젤 전체를 보이는 전시 공간 안에 배치한다. */}
        <div className="desktop-gallery-exhibits" aria-label="전시 중인 작품">
          {roomArtworks.map((artwork, index) => (
            <GalleryEasel key={artwork.challengeId} artwork={artwork}
              left={5 + (index + 0.5) * 90 / exhibitCount}
              top={57.5}
              width={easelWidth}
              isNearby={Math.abs(muPositionPercent - (5 + (index + 0.5) * 90 / exhibitCount)) <= 6} />
          ))}
        </div>
        <div ref={muRef} className="desktop-gallery-character">
          <WalkingMu direction={pose.direction} frame={pose.frame} />
        </div>
        <p className={`desktop-gallery-guide${showGuide ? '' : ' desktop-gallery-guide--hidden'}`}
          role="status" aria-hidden={!showGuide}>← → 방향키로 뮤를 움직여 보세요</p>
      </div>
      <header className="desktop-gallery-header">
        {loading ? <p role="status">작품을 불러오는 중이에요.</p>
          : error ? <div role="alert">작품을 불러오지 못했어요. <button type="button" onClick={() => {
            setLoading(true); setError(false); setRetry(value => value + 1)
          }}>다시 시도</button></div>
          : artworks.length === 0 ? <p>전시 중인 작품이 없어요.</p> : null}
        <Link to="/museum" className="desktop-gallery-return">앱 화면으로 돌아가기</Link>
      </header>
        {!loading && !error && roomCount > 1 && (
          <nav className="desktop-gallery-rooms" aria-label="전시실 이동">
            <button type="button" disabled={currentRoom === 0}
              onClick={() => setRoomIndex(Math.max(0, currentRoom - 1))}>이전 전시실</button>
            <span role="status" aria-live="polite">{currentRoom + 1} / {roomCount} 전시실</span>
            <button type="button" disabled={currentRoom === roomCount - 1}
              onClick={() => setRoomIndex(Math.min(roomCount - 1, currentRoom + 1))}>다음 전시실</button>
          </nav>
        )}

    </main>
  )
}
