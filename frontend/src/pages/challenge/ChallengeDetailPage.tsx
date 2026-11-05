import { useEffect, useState } from 'react'
import { useOutletContext, useParams } from 'react-router'
import { ChallengeStatus } from '../../components/molecules/card/ChallengeStatus'
import { ScheduleList } from '../../components/organisms/schedule/ScheduleList'
import { getChallengeDetail } from '../../api/ChallengeApi'
import type { ChallengeDetailResponse } from '../../types/Challenge'
import type { Schedule } from '../../types/schedule'

type LayoutContext = {
  setPageTitle: (title: string) => void
}

export function ChallengeDetailPage() {
  const { challengeId } = useParams()
  const { setPageTitle } = useOutletContext<LayoutContext>()

  const [details, setDetails] = useState<ChallengeDetailResponse[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false

    async function fetchChallengeDetail() {
      setLoading(true)
      setError(null)
      setDetails([])
      setPageTitle('')

      try {
        const id = Number(challengeId)

        if (!challengeId || !Number.isSafeInteger(id) || id <= 0) {
          throw new Error('올바르지 않은 도전 ID입니다.')
        }

        const data = await getChallengeDetail(id)

        if (!cancelled) {
          setDetails(data)
          setPageTitle(data[0]?.challengeTitle ?? '')
        }
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error ? err.message : '상세 조회 실패'
          )
        }
      } finally {
        if (!cancelled) {
          setLoading(false)
        }
      }
    }

    fetchChallengeDetail()

    return () => {
      cancelled = true
      setPageTitle('')
    }
  }, [challengeId, setPageTitle])

  if (loading) {
    return <div className="p-5">불러오는 중...</div>
  }

  if (error) {
    return <div className="p-5">{error}</div>
  }

  if (details.length === 0) {
    return <div className="p-5">회차 정보가 없습니다.</div>
  }

  // API 응답을 기존 Schedule 타입으로 변환
  const schedules: Schedule[] = details.map((detail) => ({
    dayNo: detail.dayNo,
    scheduleDate: detail.scheduleDate,
    status: detail.scheduleStatus,
    attemptCount: 0,
    resultReason: null,
  }))

  const sortedSchedules = [...schedules].sort(
    (a, b) => a.dayNo - b.dayNo
  )

  const successCount = schedules.filter(
    (schedule) => schedule.status === 'SUCCESS'
  ).length

  const totalDays = schedules.length

  const startDate = sortedSchedules[0].scheduleDate.slice(0, 10)
  const endDate =
    sortedSchedules[sortedSchedules.length - 1].scheduleDate.slice(0, 10)

  return (
    <div className="flex flex-col gap-5 px-5 py-6">
      <ChallengeStatus
        successCount={successCount}
        totalDays={totalDays}
        startDate={startDate}
        endDate={endDate}
      />

      <ScheduleList schedules={sortedSchedules} />
    </div>
  )
}