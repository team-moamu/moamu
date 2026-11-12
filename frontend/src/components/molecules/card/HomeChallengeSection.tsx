import type { HomeChallenge, HomeDataMode } from '../../../pages/home/homeData'
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
      <div aria-live="polite">
        {isLoading ? (
          <p className="px-2 py-[26px] text-center text-sm">도전을 불러오고 있어요.</p>
        ) : (
          <>
            {mode === 'live' && challenges.length === 0 && (
              <p className="px-2 py-[26px] text-center text-sm">이 날짜에는 도전이 없어요.</p>
            )}
            <div className="columns-2 gap-4 max-[350px]:gap-2.5">
              {challenges.map((challenge, index) => (
                <div
                  key={challenge.id}
                  className="mb-4 break-inside-avoid max-[350px]:mb-2.5"
                >
                  <HomeChallengeCard
                    challenge={challenge}
                    featured={index === 0}
                    isToday={isToday}
                  />
                </div>
              ))}
            </div>
          </>
        )}
      </div>
    </section>
  )
}
