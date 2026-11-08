import type { ScheduleStatus } from '../../types/schedule'
import type { ChallengeDetailResponse, TodayChallengeResponse } from '../../types/Challenge'

export interface HomeChallenge {
  id: number
  scheduleId: number
  title: string
  day: number
  duration: number
  pieces: number | null
  status: ScheduleStatus
  demo?: boolean
}

export type HomeRecords = Record<string, HomeChallenge[]>
export type HomeDataMode = 'loading' | 'live' | 'demo' | 'unauthenticated'

export function dateKey(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')

  return `${year}-${month}-${day}`
}

export function shiftDate(date: Date, days: number): Date {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate() + days, 12)
}

export function centeredDates(selectedDate: Date): Date[] {
  return Array.from({ length: 7 }, (_, index) => shiftDate(selectedDate, index - 3))
}

export function toHomeChallenge(
  challenge: TodayChallengeResponse,
  history: ChallengeDetailResponse[] | null,
  selectedDateKey: string,
): HomeChallenge {
  const start = new Date(`${challenge.startDate}T12:00:00`).getTime()
  const end = new Date(`${challenge.endDate}T12:00:00`).getTime()
  const millisecondsPerDay = 86_400_000

  // 상세 조회 실패를 조각 0개로 오인하지 않도록 null을 유지한다.
  const pieces = history === null ? null : history.filter(schedule => (
    schedule.scheduleDate <= selectedDateKey && schedule.scheduleStatus === 'SUCCESS'
  )).length

  return {
    id: challenge.challengeId,
    scheduleId: challenge.scheduleId,
    title: challenge.challengeTitle,
    day: challenge.dayNo,
    duration: Math.round((end - start) / millisecondsPerDay) + 1,
    pieces,
    status: challenge.scheduleStatus,
  }
}

export function demoChallenges(selectedDate: Date, today: Date): HomeChallenge[] {
  const selectedKey = dateKey(selectedDate)
  const isYesterday = selectedKey === dateKey(shiftDate(today, -1))

  if (selectedKey !== dateKey(today) && !isYesterday) {
    return []
  }

  return [
    {
      id: 1,
      scheduleId: 1,
      title: '명찰 챙기기',
      day: isYesterday ? 3 : 4,
      duration: 14,
      pieces: isYesterday ? 2 : 3,
      status: 'OPEN',
      demo: true,
    },
    {
      id: 2,
      scheduleId: 2,
      title: isYesterday ? '계단 오르기' : '물 마시기',
      day: 1,
      duration: 7,
      pieces: 0,
      status: 'OPEN',
      demo: true,
    },
  ]
}
