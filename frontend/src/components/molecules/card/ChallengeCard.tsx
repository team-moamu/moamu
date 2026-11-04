import { BlurImage } from '../../atoms/image/BlurImage'
import { Text } from '../../atoms/typography/Text'
import { SubText } from '../../atoms/typography/SubText'
import { Badge } from '../../atoms/badge/Badge'
import { SuccessButton } from '../../atoms/button/SuccessButton'

type ChallengeCardProps = {
  challengeId: number
  challengeTitle: string
  imageUrl: string
  dayNo: number
  durationDays: number
  attemptCount: number
  maxAttempts: number
  remainingTime: string
  status: 'available' | 'retry'
  onVerify: (challengeId: number) => void
}

export function ChallengeCard({
  challengeId,
  challengeTitle,
  imageUrl,
  dayNo,
  durationDays,
  attemptCount,
  maxAttempts,
  remainingTime,
  status,
  onVerify,
}: ChallengeCardProps) {
  return (
    <div className="rounded-xl p-4 shadow-sm card" style={{backgroundColor: '#fff8ed'}}>
      <div className="flex gap-4">
        <BlurImage
          src={imageUrl}
          alt={challengeTitle}
        />

        <div className="flex flex-1 flex-col gap-2">
          <Text>{challengeTitle}</Text>

          <SubText bold>
            Day {dayNo} / {durationDays} · 남은 시도{' '}
            {maxAttempts - attemptCount}/{maxAttempts}
          </SubText>

          <SubText>
            마감까지 {remainingTime}
          </SubText>

          <Badge variant={status}>
            {status === 'available'
              ? '인증 가능'
              : '재인증 가능'}
          </Badge>
        </div>
      </div>

      <div className="mt-4">
        <SuccessButton icon="camera" onClick={() => onVerify(challengeId)}>
          인증하기
        </SuccessButton>
      </div>
    </div>
  )
}