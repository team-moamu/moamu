import { Link } from 'react-router'
import { ProgressBar } from '../../atoms/progressBar/ProgressBar'
import artwork from '../../../mockTest/artwork-preview.svg'
import type { ScheduleStatus } from '../../../types/schedule'
import type { HomeChallenge } from '../../../pages/home/homeData'

const statusLabels: Record<ScheduleStatus, string> = {
  UPCOMING: '시작 예정',
  OPEN: '인증하기',
  PROGRESSING: '인증 진행 중',
  IN_REVIEW: '검토 중',
  SUCCESS: '인증 완료',
  FAILED: '인증 실패',
  PENDING_SETTLEMENT: '결과 확인 중',
}

interface HomeChallengeCardProps {
  challenge: HomeChallenge
  featured: boolean
  isToday: boolean
}

export function HomeChallengeCard({ challenge, featured, isToday }: HomeChallengeCardProps) {
  const appearance = featured
    ? 'row-span-2 min-h-[244px] bg-[var(--color-surface-soft)]'
    : 'bg-[#dfe8d3]'
  const progressLabel = challenge.status === 'UPCOMING' ? '시작 예정' : '진행 중'
  const dayDescription = challenge.status === 'SUCCESS' ? '조각을 모았어요' : '차곡차곡 모으는 중'

  return (
    <article
      className={`flex min-w-0 flex-col rounded-[9px] p-3 max-[350px]:p-2.5 ${appearance}`}
    >
      <p className="m-0 mb-[7px] text-xs">
        {progressLabel} · {challenge.duration}일 도전
      </p>
      <h3
        className="m-0 mb-1.5 font-[family-name:var(--font-description)]
          text-xl leading-[1.2] font-bold"
      >
        {challenge.title}
      </h3>
      <p className="m-0 mb-3 text-xs">{challenge.day}일 차 {dayDescription}</p>
      {featured && <ChallengeArtwork challenge={challenge} />}
      <ChallengeAction challenge={challenge} isToday={isToday} />
    </article>
  )
}

function ChallengeArtwork({ challenge }: { challenge: HomeChallenge }) {
  return (
    <div className="mt-px mb-[22px] flex items-center gap-[9px]">
      <div className="min-w-0 flex-1">
        <strong className="mb-1 block text-xs text-[var(--color-primary)]">
          {challenge.pieces ?? '—'} / {challenge.duration} 조각
        </strong>
        {challenge.pieces !== null && (
          <ProgressBar value={challenge.pieces} max={challenge.duration} />
        )}
      </div>
      <img
        src={artwork}
        alt="작품 미리보기"
        className="aspect-square w-[48%] rounded-[9px] border-2 border-[#cebd9e] object-cover"
      />
    </div>
  )
}

function ChallengeAction({ challenge, isToday }: { challenge: HomeChallenge; isToday: boolean }) {
  const canVerify = isToday && ['OPEN', 'PROGRESSING', 'FAILED'].includes(challenge.status)
  const destination = canVerify
    ? `/challenge/${challenge.id}/verification?scheduleId=${challenge.scheduleId}`
    : `/challenge/${challenge.id}`
  const label = isToday ? statusLabels[challenge.status] : '기록 보기'
  const actionClassName = `mt-auto flex min-h-[38px] items-center self-end pt-2
    text-sm text-[var(--color-text)] no-underline`

  return (
    <Link className={actionClassName} to={destination}>
      {label} →
    </Link>
  )
}
