import type { ScheduleStatus } from "./schedule"

// 인증 제출 페이지 조회
export type ChallengeVerificationResponse = {
  challengeId: number
  challengeTitle: string
  scheduleId: number
  dayNo: number
  dayCount: number
  attemptCount: number
  description: string[]
}

// 오늘의 도전 목표 조회
export type TodayChallengeResponse = {
  challengeId: number
  challengeTitle: string
  startDate: string
  endDate: string
  scheduleId: number,
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

// 도전 생성 요청
export interface ChallengeCreateRequest {
  goalTemplateId: number
  startDate: string
  durationDays: number
}
