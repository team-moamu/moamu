import { useNavigate } from 'react-router'
import { ChallengeCardList } from '../../components/organisms/puzzle/ChallengeCardList'
import backgroundMu from '../../assets/images/background_mu.png'

type Challenge = React.ComponentProps<
  typeof ChallengeCardList
>['challenges'][number]

export function ChallengeListPage() {
  const navigate = useNavigate()

  // TODO: API 연결 후 실제 데이터로 변경
  const challenges: Challenge[] = [
    {
      challengeId: 1,
      challengeTitle: '명찰 챙기기',
      imageUrl: '/images/artwork1.png',
      dayNo: 3,
      durationDays: 14,
      attemptCount: 1,
      maxAttempts: 3,
      remainingTime: '5시간 12분',
      status: 'available',
    },
    {
      challengeId: 2,
      challengeTitle: '알고리즘 공부하기',
      imageUrl: '/images/artwork2.png',
      dayNo: 5,
      durationDays: 7,
      attemptCount: 2,
      maxAttempts: 3,
      remainingTime: '3시간 20분',
      status: 'retry',
    },
  ]

  const handleVerify = (challengeId: number) => {
    navigate(`/challenge/${challengeId}/verify`)
  }

  return (
    <>
        <div className="relative w-full">
      <img
        src={backgroundMu}
        alt="뮤 이미지"
        className="block w-full"
      />

      <p className="text-description absolute top-[30%] left-1/2 -translate-x-1/2">
        10월 2일 금요일
      </p>
    </div>

    <div className="flex flex-col">

      <ChallengeCardList
        challenges={challenges}
        onVerify={handleVerify}
      />
    </div>
    </>
  )
}