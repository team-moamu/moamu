import { BlurImage } from '../../atoms/image/BlurImage'
import { Text } from '../../atoms/typography/Text'
import { SubText } from '../../atoms/typography/SubText'
import { Badge } from '../../atoms/badge/Badge'
import { SuccessButton } from '../../atoms/button/SuccessButton'
import { Icon } from '../../atoms/icon/Icon'

type ChallengeCardProps = {
  challengeId: number
  scheduleId: number
  challengeTitle: string
  imageUrl: string
  imageAlreadyBlurred?: boolean
  dayNo: number
  durationDays: number
  attemptCount: number
  maxAttempts: number
  remainingTime: string
  status: 'available' | 'retry' | 'success' | 'fail'
  onVerify: (challengeId: number, scheduleId: number) => void
  onClick?: () => void
}

export function ChallengeCard({
  challengeId,
  scheduleId,
  challengeTitle,
  imageUrl,
  imageAlreadyBlurred,
  dayNo,
  durationDays,
  attemptCount,
  maxAttempts,
  remainingTime,
  status,
  onVerify,
  onClick,
}: ChallengeCardProps) {
  return (
      <div
        onClick={onClick}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.target !== e.currentTarget) return

          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault()
            onClick?.()
          }
        }}
        className="rounded-md p-4 shadow-sm card"
      >
      <div className="flex gap-4">
        <BlurImage
          src={imageUrl}
          alt={challengeTitle}
          alreadyBlurred={imageAlreadyBlurred}
        />

        <div className="flex flex-1 flex-col gap-2">
          <Text>{challengeTitle}</Text>

          <SubText bold>
            Day {dayNo} / {durationDays} · 남은 시도{' '}
            {Math.max(0, maxAttempts - attemptCount)}/{maxAttempts}
          </SubText>

          <SubText>
            <Icon name="clock" size={12} className='mr-1' />
            마감까지 {remainingTime}
          </SubText>

          <Badge variant={status}>
            {status === 'available'
              ? '인증 가능'
              : status === 'success' ? '성공'
                : status === 'fail' ? '실패' : '재인증 가능'}
          </Badge>
        </div>
      </div>

      <div className="mt-4">
        <SuccessButton
          icon="camera"
          disabled={status === 'success' || status === 'fail'}
          disabledTone={status === 'success' ? 'blue' : 'neutral'}
          onClick={(e) => {
            e.stopPropagation()
            onVerify(challengeId, scheduleId)
          }}
        >
          {status === 'retry' ? '재인증하기' : '인증 완료'}
        </SuccessButton>
      </div>
    </div>
  )
}
