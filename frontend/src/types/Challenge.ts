import type { ScheduleStatus } from "./schedule"

// 오늘의 도전 목표 조회
export type TodayChallengeResponse = {
  challengeId: number
  challengeTitle: string
  startDate: string
  endDate: string
  dayNo: number
  scheduleStatus:
  | 'UPCOMING'
  | 'OPEN'
  | 'PROGRESSING'
  | 'IN_REVIEW'
  | 'SUCCESS'
  | 'FAILED'
  | 'PENDING_SETTLEMENT'
  attemptCount: number
  resultReason: string | null
}

// 도전 회차 상세 조회
export type ChallengeDetailResponse = {
  challengeId: number
  challengeTitle: string
  scheduleId: number
  dayNo: number
  scheduleDate: string
  scheduleStatus: ScheduleStatus
}