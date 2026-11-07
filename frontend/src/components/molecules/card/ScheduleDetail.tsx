import { Badge } from '../../atoms/badge/Badge'
import { scheduleStatusMap } from '../../../utils/scheduleStatus'
import type { ScheduleStatus } from '../../../types/schedule'

type ScheduleDetailProps = {
  dayNo: number
  scheduleDate: string
  status: ScheduleStatus
}

export function ScheduleDetail({
  dayNo,
  scheduleDate,
  status,
}: ScheduleDetailProps) {
  const { label, variant } = scheduleStatusMap[status]

  const formattedDate = new Date(
    `${scheduleDate.slice(0, 10)}T00:00:00`
  ).toLocaleDateString('ko-KR', {
    month: 'numeric',
    day: 'numeric',
    weekday: 'short',
  })

  return (
    <div className="flex items-center justify-between border-b border-[#E8E0D5] px-4 py-3 last:border-b-0">
      <div className="flex items-center gap-4">
        <span className="w-12 text-sm font-semibold text-[#4A3B30]">
          Day {dayNo}
        </span>

        <span className="text-sm text-[#8B7B6B]">
          {formattedDate}
        </span>
      </div>

      <Badge variant={variant}>{label}</Badge>
    </div>
  )
}