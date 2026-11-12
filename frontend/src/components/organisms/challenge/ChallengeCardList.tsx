import { ChallengeCard } from '../../molecules/card/ChallengeCard'

type Challenge = Omit<
  React.ComponentProps<typeof ChallengeCard>,
  'onVerify' | 'onClick'
>

type ChallengeCardListProps = {
  challenges: Challenge[]
  onVerify: (challengeId: number, scheduleId: number) => void
  onCardClick?: (challengeId: number) => void
}

export function ChallengeCardList({
  challenges,
  onVerify,
  onCardClick,
}: ChallengeCardListProps) {
  return (
    <div className="flex flex-col gap-4">
      {challenges.map((challenge) => (
        <ChallengeCard
          key={challenge.challengeId}
          {...challenge}
          onVerify={onVerify}
          onClick={() => onCardClick?.(challenge.challengeId)}
        />
      ))}
    </div>
  )
}