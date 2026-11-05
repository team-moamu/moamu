import { ScheduleDetail } from '../../molecules/card/ScheduleDetail'
import type { Schedule } from '../../../types/schedule'

type ScheduleListProps = {
  schedules: Schedule[]
}

export function ScheduleList({ schedules }: ScheduleListProps) {
  return (
    <div className="overflow-hidden rounded-xl border border-[#E8E0D5] bg-white">
      {schedules.map((schedule) => (
        <ScheduleDetail
          key={schedule.dayNo}
          dayNo={schedule.dayNo}
          scheduleDate={schedule.scheduleDate}
          status={schedule.status}
        />
      ))}
    </div>
  )
}