import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'

import { ChallengeCardList } from '../../components/organisms/puzzle/ChallengeCardList'
import { getTodayChallenges } from '../../api/ChallengeApi'
import backgroundMu from '../../assets/images/background_mu.png'
import { getRemainingTime } from '../../utils/time'

type Challenge = React.ComponentProps<
  typeof ChallengeCardList
>['challenges'][number]

export function ChallengeListPage() {
  const navigate = useNavigate()

  const [challenges, setChallenges] = useState<Challenge[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const today = new Date()

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
          challengeTitle: challenge.challengeTitle,
          imageUrl: '/images/default-artwork.png',
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

  const handleVerify = (challengeId: number) => {
    navigate(`/challenge/${challengeId}/verify`)
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
          />
        )}
      </div>
    </>
  )
}