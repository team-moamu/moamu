import type { ScheduleStatus } from "./schedule"

// 도전 전체 완료 결과. 일일 인증 성공 응답과 구분
export type ChallengeCompletionResponse = {
  challengeId: number
  challengeTitle: string
  status: 'COMPLETED'
  startDate: string
  endDate: string
  durationDays: number
  artwork: { artworkId: number; title: string | null; imageUrl: string | null }
  gridRows: number
  gridCols: number
  revealedPieceIndices: number[]
  totalPieceCount: number
  earnedPieceCount: number
  missingPieceCount: number
  allPiecesEarned: boolean
}

// 인증 성공 화면 조회 응답
export type VerificationSuccessResponse = {
  verificationId: number
  status: 'SUCCESS'
  challengeId: number
  challengeTitle: string
  scheduleId: number
  dayNo: number
  scheduleStatus: ScheduleStatus
  artwork: { artworkId: number; title: string | null; imageUrl: string | null }
  gridRows: number
  gridCols: number
  openedPieceIndex: number
  revealedPieceIndices: number[]
  earnedPieceCount: number
  totalPieceCount: number
  checks: (VerificationSubmitResponse['checks'][number] & { conditionId: number; description: string })[]
}

// 사진 업로드 및 AI 인증 결과
export type VerificationSubmitResponse = {
  verificationId: number
  mediaId: number
  aiTaskId: number
  status: 'SUCCESS' | 'FAILED' | 'REVIEW_REQUIRED'
  checks: { code: string; status: 'MET' | 'NOT_MET' | 'UNCERTAIN'; reason: string }[]
}

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
