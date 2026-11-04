import { ChallengeCard } from '../../molecules/card/ChallengeCard'

type Challenge = Omit<
  React.ComponentProps<typeof ChallengeCard>,
  'onVerify'
>

type ChallengeCardListProps = {
  challenges: Challenge[]
  onVerify: (challengeId: number) => void
}

export function ChallengeCardList({
  challenges,
  onVerify,
}: ChallengeCardListProps) {
  return (
    <div className="flex flex-col gap-4">
      {challenges.map((challenge) => (
        <ChallengeCard
          key={challenge.challengeId}
          {...challenge}
          onVerify={onVerify}
        />
      ))}
    </div>
  )
}