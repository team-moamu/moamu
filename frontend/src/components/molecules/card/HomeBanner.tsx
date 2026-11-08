import type { HomeChallenge, HomeDataMode } from '../../../pages/home/homeData'

const tileColors = [
  'bg-[#305536]', 'bg-[#dfe8d3]', 'bg-[#e9b964]',
  'bg-[#dfe8d3]', 'bg-[#e9b964]', 'bg-[#e5eff1]',
  'bg-[#dfe8d3]', 'bg-[#305536]', 'bg-[#305536]',
]

interface HomeGalleryProps {
  challenges: HomeChallenge[]
  mode: HomeDataMode
  isToday: boolean
}

export function HomeGallery({ challenges, mode, isToday }: HomeGalleryProps) {
  const earnedPieces = challenges.filter(challenge => challenge.status === 'SUCCESS').length
  let pieceSummary = `${earnedPieces} / ${challenges.length}`

  if (mode === 'loading') {
    pieceSummary = '…'
  } else if (challenges.length > 0) {
    pieceSummary = '1 / 3'
  }

  return (
    <section
      aria-label="나의 작은 미술관"
      className="relative overflow-hidden rounded-[11px] px-[13px] py-5
        bg-[var(--color-primary)] text-white max-[350px]:pl-[11px]"
    >
      <p className="m-0 text-sm">나의 작은 미술관</p>
      <h2
        className="mt-2.5 mb-3 max-w-[72%] font-[family-name:var(--font-description)]
          text-[clamp(23px,6.4vw,29px)] leading-[1.3] font-bold tracking-[-1px]
          max-[350px]:text-[23px]"
      >
        작은 습관, 큰 즐거움<br />
        <strong>모아뮤</strong>와 함께해요
      </h2>
      <p className="m-0 text-sm">
        {isToday ? '오늘의' : '선택한 날의'} 조각
        <strong className="ml-2 text-xl">{pieceSummary}</strong>
      </p>
      <div
        aria-hidden="true"
        className="absolute top-8 right-[13px] grid w-[28%] grid-cols-3 gap-[5px]
          max-[350px]:right-2.5 max-[350px]:gap-1"
      >
        {tileColors.map((color, index) => (
          <span key={index} className={`aspect-square rounded-[9px] ${color}`} />
        ))}
      </div>
    </section>
  )
}
