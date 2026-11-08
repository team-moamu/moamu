import { Link } from 'react-router'
import type { HomeChallenge, HomeDataMode } from '../homeData'
import { HomeChallengeCard } from './HomeChallengeCard'

interface HomeChallengeSectionProps {
  challenges: HomeChallenge[]
  mode: HomeDataMode
  isToday: boolean
}

export function HomeChallengeSection({ challenges, mode, isToday }: HomeChallengeSectionProps) {
  const isLoading = mode === 'loading'

  return (
    <section className="mt-[18px]" aria-labelledby="challenges-heading" aria-busy={isLoading}>
      <h2
        id="challenges-heading"
        className="mx-2 mt-0 mb-2.5 font-[family-name:var(--font-description)] text-[19px] font-bold"
      >
        나의 도전
      </h2>
      {mode === 'demo' && (
        <p className="mx-2 -mt-[3px] mb-[9px] text-[11px] text-[var(--color-text-muted)]">
          서버에 연결할 수 없어 예시 기록을 표시해요.
        </p>
      )}
      <div aria-live="polite">
        {isLoading ? (
          <p className="px-2 py-[26px] text-center text-sm">도전을 불러오고 있어요.</p>
        ) : (
          <>
            {challenges.length === 0 && (
              <p className="px-2 py-[26px] text-center text-sm">이 날짜에는 도전이 없어요.</p>
            )}
            <div className="grid grid-cols-2 items-stretch gap-4 max-[350px]:gap-2.5">
              {challenges.map((challenge, index) => (
                <HomeChallengeCard
                  key={challenge.id}
                  challenge={challenge}
                  featured={index === 0}
                  isToday={isToday}
                />
              ))}
              <CreateChallengeLink />
            </div>
          </>
        )}
      </div>
    </section>
  )
}

function CreateChallengeLink() {
  return (
    <Link
      to="/challenge/create"
      className="flex min-h-[84px] min-w-0 items-center justify-center gap-[7px]
        rounded-[9px] border border-[var(--color-border)] px-[7px] py-3
        font-[family-name:var(--font-description)] text-lg font-bold
        text-[var(--color-text)] no-underline shadow-[0_0_4px_#49372526]
        max-[350px]:gap-0.5 max-[350px]:text-[15px]"
    >
      <span
        aria-hidden="true"
        className="font-[family-name:var(--font-ui)] text-[42px] leading-none font-normal"
      >
        ＋
      </span>
      새 도전 만들기
    </Link>
  )
}
