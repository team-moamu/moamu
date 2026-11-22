const images = import.meta.glob('../../../assets/images/gallery/walkingMu/*/*.png', {
  eager: true, query: '?url', import: 'default',
}) as Record<string, string>
const frames = Object.entries(images).sort(([a], [b]) => a.localeCompare(b))
const right = frames.filter(([path]) => path.includes('/right/')).map(([, url]) => url)
const left = frames.filter(([path]) => path.includes('/left/')).map(([, url]) => url)

// 방향키 입력에 맞는 방향과 걷기 프레임만 표시한다.
export function WalkingMu({ direction, frame }: { direction: 'left' | 'right'; frame: number }) {
  const activeFrames = direction === 'right' ? right : left
  return <img className="desktop-gallery-mu" src={activeFrames[frame % activeFrames.length]}
    alt="미술관을 걷는 뮤" draggable={false} />
}
