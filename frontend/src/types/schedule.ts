export type ScheduleStatus =
  | 'UPCOMING'
  | 'OPEN'
  | 'PROGRESSING'
  | 'IN_REVIEW'
  | 'SUCCESS'
  | 'FAILED'
  | 'PENDING_SETTLEMENT'

export type Schedule = {
  dayNo: number
  scheduleDate: string
  status: ScheduleStatus
  attemptCount: number
  resultReason: string | null
}