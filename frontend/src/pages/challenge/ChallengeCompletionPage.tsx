import { useEffect, useRef, useState } from 'react'
import type { CSSProperties } from 'react'
import { Link, useParams } from 'react-router'

import { ChallengeCompletionError, getChallengeCompletion } from '../../api/ChallengeApi'
import celebrationMu from '../../assets/images/complete_mu.png'
import { ArtworkPuzzle } from '../../components/organisms/puzzle/ArtworkPuzzle'
import { Icon } from '../../components/atoms/icon/Icon'
import { BottomNavigation } from '../../components/layout/navigation/BottomNavigation'
import type { ChallengeCompletionResponse } from '../../types/Challenge'
import './ChallengeCompletionPage.css'

interface ChallengeCompletionPageProps {
  accessToken?: string
}

type CompletionPhase = 'loading' | 'celebration' | 'transition' | 'result' | 'error'
const CELEBRATION_DURATION_MS = 4500
const TRANSITION_DURATION_MS = 600
const confettiColors = ['#9dc46b', '#6fbb92', '#f0c56a', '#af86c8', '#df8795']

// 렌더링마다 위치가 바뀌지 않도록 입자별 고정 난수 사용
function confettiVariation(index: number, seed: number) {
  const value = Math.sin((index + 1) * 127.1 + seed * 311.7) * 43758.5453
  return value - Math.floor(value)
}

// 완료 조회 성공 후에만 축하 화면 진입. 재방문 시 다시 축하하며 별도 확인 기록은 저장하지 않음
export function ChallengeCompletionPage({ accessToken }: ChallengeCompletionPageProps) {
  const { challengeId } = useParams()
  const [result, setResult] = useState<ChallengeCompletionResponse | null>(null)
  const [phase, setPhase] = useState<CompletionPhase>('loading')
  const [error, setError] = useState<string | null>(null)
  const [canRetry, setCanRetry] = useState(true)
  const [reloadCount, setReloadCount] = useState(0)
  const resultHeading = useRef<HTMLHeadingElement>(null)

  useEffect(() => {
    const controller = new AbortController()
    async function fetchCompletion() {
      setPhase('loading')
      setResult(null)
      setError(null)
      setCanRetry(true)
      try {
        const id = Number(challengeId)
        if (!challengeId || !Number.isSafeInteger(id) || id <= 0) {
          throw new ChallengeCompletionError('올바르지 않은 도전 ID입니다.', 'INVALID_REQUEST')
        }
        const response = await getChallengeCompletion(id, controller.signal, accessToken)
        if (!controller.signal.aborted) {
          setResult(response)
          setPhase('celebration')
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err instanceof Error ? err.message : '완료 결과를 불러오지 못했어요.')
          setCanRetry(!(err instanceof ChallengeCompletionError)
            || !['INVALID_REQUEST', 'RESOURCE_NOT_FOUND', 'UNAUTHORIZED', 'AUTHENTICATION_REQUIRED'].includes(err.code ?? ''))
          setPhase('error')
        }
      }
    }
    fetchCompletion()
    return () => controller.abort()
  }, [challengeId, accessToken, reloadCount])

  // 축하 화면 4.5초 표시 후 부드럽게 전환. 이탈·ID 변경 시 각 단계의 타이머 정리
  useEffect(() => {
    if ((phase !== 'celebration' && phase !== 'transition') || !result || result.challengeId !== Number(challengeId)) return
    const timer = window.setTimeout(
      () => setPhase(phase === 'celebration' ? 'transition' : 'result'),
      phase === 'celebration' ? CELEBRATION_DURATION_MS : TRANSITION_DURATION_MS,
    )
    return () => window.clearTimeout(timer)
  }, [phase, result, challengeId])

  useEffect(() => {
    if (phase === 'result') {
      resultHeading.current?.focus({ preventScroll: true })
      window.scrollTo(0, 0)
    }
  }, [phase])

  if (phase === 'loading' || (result && result.challengeId !== Number(challengeId))) {
    return <main className="flex min-h-dvh flex-col items-center justify-center gap-5 bg-[var(--color-background)] px-6 py-10 text-center" role="status"><p className="text-sm leading-6">완성된 기록을 불러오고 있어요.</p></main>
  }

  if (phase === 'error' || !result) {
    return (
      <main className="flex min-h-dvh flex-col items-center justify-center gap-5 bg-[var(--color-background)] px-6 py-10 text-center">
        <h1 className="[font-family:var(--font-description)] text-[26px] font-bold leading-[1.45]">완료 결과를 확인해 주세요</h1>
        <p className="text-sm leading-6" role="alert">{error || '완료 결과를 불러오지 못했어요.'}</p>
        {canRetry && <button className="cursor-pointer rounded-lg bg-[var(--color-primary)] px-6 py-3 text-white" type="button" onClick={() => setReloadCount(count => count + 1)}>다시 불러오기</button>}
        <Link to="/challenge/list">도전 목록으로 돌아가기</Link>
      </main>
    )
  }

  return (
    <div className="relative min-h-dvh">
      <div className={phase === 'transition' ? 'animate-[completion-result-enter_600ms_ease_both] motion-reduce:animate-none' : undefined} hidden={phase === 'celebration'} inert={phase !== 'result'} aria-hidden={phase !== 'result'}>
        <div className="min-h-dvh bg-white pb-[calc(96px+env(safe-area-inset-bottom))] text-[var(--color-text)]">
          <header className="bg-[var(--color-background)] px-6 pt-[max(40px,env(safe-area-inset-top))] pb-5 text-center">
            <h1 className="[font-family:var(--font-description)] text-[26px] font-bold leading-[1.45] focus:outline-none" ref={resultHeading} tabIndex={-1}>도전 완료</h1>
          </header>
          <main className="px-5 pt-6 pb-10 max-[350px]:px-4">
            <ArtworkPuzzle
              imageUrl={result.artwork.imageUrl}
              title={result.artwork.title}
              gridRows={result.gridRows}
              gridCols={result.gridCols}
              totalPieceCount={result.totalPieceCount}
              revealedPieceIndices={result.revealedPieceIndices}
              allPiecesEarned={result.allPiecesEarned}
              maskColor="black"
            />
            <section className="my-5 text-center" aria-label="도전 기록">
              <h2 className="[font-family:var(--font-description)] text-[19px] font-bold">{result.challengeTitle}</h2>
              <p className="mt-1 text-xs leading-6">{result.durationDays}일 동안의 기록이 하나의 작품이 되었어요.</p>
              <p className="mt-2 text-[11px] text-[var(--color-text-muted)]">{result.startDate} ~ {result.endDate}</p>
            </section>
            <dl className="grid grid-cols-2 gap-3">
              <div className="flex items-center justify-center gap-3.5 rounded-[9px] border border-[var(--color-primary)] bg-[#e1e9d5] px-2 py-4 max-[350px]:gap-2">
                <Icon name="challenge" size={30} className="text-[var(--color-primary)]" />
                <div><dt className="[font-family:var(--font-description)] text-base font-bold">성공 조각</dt><dd className="mt-0.5 text-[23px] font-semibold leading-[1.3]">{result.earnedPieceCount}개</dd></div>
              </div>
              <div className="flex items-center justify-center gap-3.5 rounded-[9px] border border-[var(--color-border)] px-2 py-4 max-[350px]:gap-2">
                <Icon name="challenge" size={30} className="text-[#d8d6cf]" />
                <div><dt className="[font-family:var(--font-description)] text-base font-bold">미획득 조각</dt><dd className="mt-0.5 text-[23px] font-semibold leading-[1.3]">{result.missingPieceCount}개</dd></div>
              </div>
            </dl>
            <p className="mt-6 rounded-[10px] bg-[var(--color-background)] p-4 text-center text-[13px] leading-[1.8]">
              {result.allPiecesEarned ? '모든 조각을 모아 작품 전체가 완성되었어요.'
                : '공개된 조각은 쌓아온 기록이에요. 미획득 조각은 검정으로 남겨두었어요.'}
            </p>
          </main>
          <BottomNavigation />
        </div>
      </div>
      {(phase === 'celebration' || phase === 'transition') && (
        <main className={`absolute inset-0 z-100 flex min-h-dvh flex-col items-center justify-center gap-12 overflow-hidden bg-[#4c4b46] px-7 py-20 text-center text-[#fff8ec] transition-opacity duration-[600ms] ease-[ease] motion-reduce:transition-none ${phase === 'transition' ? 'pointer-events-none opacity-0' : 'opacity-100'}`} aria-hidden={phase === 'transition'} aria-label="작품 완성 축하">
          <div className="pointer-events-none absolute inset-0 z-0 motion-reduce:hidden" aria-hidden="true">
            {/* 화면 전체를 채우되 크기·속도·흔들림·회전은 입자마다 불규칙하게 설정 */}
            {Array.from({ length: 96 }, (_, index) => {
              const variation = (seed: number) => confettiVariation(index, seed)
              const duration = 5.8 + variation(1) * 4.2
              const progress = (Math.floor(index / 12) + variation(2)) / 8
              const flutterDuration = 1.6 + variation(3) * 2.5
              return (
                <span key={index} className="absolute -top-5 h-[var(--piece-height)] w-[var(--piece-width)] animate-[completion-confetti-fall_5s_linear_infinite_both] opacity-0 [will-change:transform,opacity] motion-reduce:animate-none" style={{
                  left: `${(index % 12 + variation(4)) / 12 * 100}%`,
                  '--drift': `${(variation(5) - 0.5) * 55}px`,
                  '--tilt': `${variation(6) * 360}deg`,
                  '--sway': `${8 + variation(7) * 24}px`,
                  '--turn': `${(variation(8) - 0.5) * 160}deg`,
                  '--piece-width': `${4 + variation(9) * 6}px`,
                  '--piece-height': `${5 + variation(10) * 9}px`,
                  '--piece-radius': variation(11) < 0.25 ? '50%' : '2px',
                  '--piece-color': confettiColors[Math.floor(variation(12) * confettiColors.length)],
                  '--piece-opacity': 0.5 + variation(13) * 0.4,
                  '--flutter-duration': `${flutterDuration}s`,
                  '--flutter-delay': `${-variation(14) * flutterDuration}s`,
                  animationDelay: `${-progress * duration}s`,
                  animationDuration: `${duration}s`,
                } as CSSProperties}><i className="block h-full w-full animate-[completion-confetti-flutter_var(--flutter-duration)_ease-in-out_infinite_alternate_both] bg-[var(--piece-color)] [animation-delay:var(--flutter-delay)] [border-radius:var(--piece-radius)] motion-reduce:animate-none" /></span>
              )
            })}
          </div>
          <div className="relative z-0 mt-10 w-full max-w-[280px]">
            <img className="relative z-1 w-full" src={celebrationMu} alt="작품 완성을 축하하는 뮤" />
          </div>
          <div className="relative z-1" role="status">
            <h1 className="[font-family:var(--font-description)] text-[26px] font-bold leading-[1.45]">{result.durationDays}일의 기록이<br />작품이 되었어요!</h1>
            <p className="mt-[18px] text-[13px]">꾸준한 노력이 멋진 작품을 만들었어요.</p>
            <p className="mt-3 text-[11px] text-[#ded9ce]">잠시 후 완성된 기록을 보여드릴게요.</p>
          </div>
        </main>
      )}
    </div>
  )
}
