import { ChallengeStatus } from '../../components/molecules/card/ChallengeStatus'
import { ScheduleList } from '../../components/organisms/schedule/ScheduleList'
import type { Schedule } from '../../types/schedule'

// 임시 데이터 (API 연결 후 삭제)
const schedules: Schedule[] = [
  {
    dayNo: 1,
    scheduleDate: '2026-09-30',
    status: 'SUCCESS',
    attemptCount: 1,
    resultReason: null,
  },
  {
    dayNo: 2,
    scheduleDate: '2026-10-01',
    status: 'FAILED',
    attemptCount: 1,
    resultReason: '인증 실패',
  },
  {
    dayNo: 3,
    scheduleDate: '2026-10-02',
    status: 'SUCCESS',
    attemptCount: 1,
    resultReason: null,
  },
  {
    dayNo: 4,
    scheduleDate: '2026-10-03',
    status: 'PROGRESSING',
    attemptCount: 1,
    resultReason: null,
  },
  ...Array.from({ length: 10 }, (_, index) => ({
    dayNo: index + 5,
    scheduleDate: `2026-10-${String(index + 4).padStart(2, '0')}`,
    status: 'UPCOMING' as const,
    attemptCount: 0,
    resultReason: null,
  })),
]

export function ChallengeDetailPage() {
  const successCount = schedules.filter(
    (schedule) => schedule.status === 'SUCCESS'
  ).length

  const totalDays = schedules.length

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <ChallengeStatus
        successCount={successCount}
        totalDays={totalDays}
        startDate="2026-09-30"
        endDate="2026-10-13"
      />

      <ScheduleList schedules={schedules} />
    </div>
  )
}