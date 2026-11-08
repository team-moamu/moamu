import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router'

import { ChallengeCardList } from '../../components/organisms/challenge/ChallengeCardList'
import { getTodayChallenges } from '../../api/ChallengeApi'
import backgroundMu from '../../assets/images/background_mu.png'
import { goalArtworkImages } from '../../constants/goalArtworkImages'
import { getRemainingTime } from '../../utils/time'
import { Icon } from '../../components/atoms/icon/Icon'

type Challenge = React.ComponentProps<
  typeof ChallengeCardList
>['challenges'][number]

export function ChallengeListPage() {
  const navigate = useNavigate()

  const [challenges, setChallenges] = useState<Challenge[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const today = new Date()

  const handleChallengeClick = (challengeId: number) => {
    navigate(`/challenge/${challengeId}`)
  }

  const formattedDate = new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    weekday: 'long',
  }).format(today)

  useEffect(() => {
    let cancelled = false

    const fetchChallenges = async () => {
      try {
        const data = await getTodayChallenges()

        if (cancelled) return

        const mappedChallenges: Challenge[] = data.map((challenge) => ({
          challengeId: challenge.challengeId,
          scheduleId: challenge.scheduleId,
          challengeTitle: challenge.challengeTitle,
          imageUrl: goalArtworkImages[challenge.goalTemplateId] ?? backgroundMu,
          dayNo: challenge.dayNo,
          durationDays: 14,
          attemptCount: challenge.attemptCount,
          maxAttempts: 3,
          remainingTime: getRemainingTime(),
          status: challenge.attemptCount > 0 ? 'retry' : 'available',
        }))

        setChallenges(mappedChallenges)
      } catch (err) {
        if (!cancelled) {
          console.error(err)
          setError('도전 목록을 불러오지 못했습니다.')
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    fetchChallenges()

    return () => {
      cancelled = true
    }
  }, [])

  const handleVerify = (challengeId: number, scheduleId: number) => {
    navigate(`/challenge/${challengeId}/verification?scheduleId=${scheduleId}`)
  }

  return (
    <>
      <div className="relative w-full">
        <img
          src={backgroundMu}
          alt=""
          className="block w-full"
        />

        <p className="absolute top-[30%] left-1/2 -translate-x-1/2 whitespace-nowrap text-sm font-medium text-[#4A382A]">
          {formattedDate}
        </p>
      </div>

      <div className="flex flex-col px-6 py-6">
        {loading ? (
          <p>도전 목록을 불러오는 중입니다.</p>
        ) : error ? (
          <p>{error}</p>
        ) : (
          <ChallengeCardList
            challenges={challenges}
            onVerify={handleVerify}
            onCardClick={handleChallengeClick}
          />
        )}
      </div>
      <div className="pointer-events-none fixed bottom-28 left-1/2 z-40 flex w-full max-w-[430px] -translate-x-1/2 justify-end px-4">
        <Link
          to="/challenge/create"
          aria-label="도전 생성하기"
          title="도전 생성하기"
          className="pointer-events-auto flex h-12 w-12 items-center justify-center rounded-full border-2 border-white bg-[var(--color-primary)] text-white shadow-md ring-2 ring-[var(--color-primary)] transition-colors hover:bg-[var(--color-primary-hover)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-primary)] active:bg-[var(--color-primary-pressed)]"
        >
          <Icon name="palette" size={36} />
        </Link>
      </div>
    </>
  )
}
