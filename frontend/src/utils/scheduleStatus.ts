import type { ScheduleStatus } from '../types/schedule'
import type { BadgeVariant } from '../components/atoms/badge/Badge'

export const scheduleStatusMap = {
  UPCOMING: { label: '예정', variant: 'scheduled' },
  OPEN: { label: '인증 가능', variant: 'available' },
  PROGRESSING: { label: '처리 중', variant: 'processing' },
  IN_REVIEW: { label: '검토 중', variant: 'review' },
  SUCCESS: { label: '성공', variant: 'success' },
  FAILED: { label: '실패', variant: 'fail' },
  PENDING_SETTLEMENT: {
    label: '정산 대기',
    variant: 'settlement',
  },
} as const satisfies Record<
  ScheduleStatus,
  { label: string; variant: BadgeVariant }
>