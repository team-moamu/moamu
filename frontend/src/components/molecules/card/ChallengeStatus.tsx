import { ProgressBar } from '../../atoms/progressBar/ProgressBar'

type ChallengeStatusProps = {
  successCount: number
  totalDays: number
  startDate: string
  endDate: string
}

export function ChallengeStatus({
  successCount,
  totalDays,
  startDate,
  endDate,
}: ChallengeStatusProps) {
  return (
    <div className="border border-[#E8E0D5] overflow-hidden p-5 rounded-md shadow-sm bg-[#fff8ed]">
      <div className="mb-3 flex items-center justify-between">
        <span className="text-sm text-[#6B5B4B]">
          도전 진행 현황
        </span>

        <span className="text-sm font-semibold text-[#4F7652]">
          {totalDays}회 중 {successCount}회 성공
        </span>
      </div>

      <ProgressBar value={successCount} max={totalDays} />

      <p className="mt-3 text-xs text-[#8B7B6B]">
        {startDate} ~ {endDate}
      </p>
    </div>
  )
}