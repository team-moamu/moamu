export type TodayChallengeResponse = {
  challengeId: number
  challengeTitle: string
  startDate: string
  endDate: string
  dayNo: number
  scheduleStatus: 'UPCOMING | OPEN | PROGRESSING | IN_REVIEW | SUCCESS | FAILED | PENDING_SETTLEMENT'
  attemptCount: number
  resultReason: string | null
}