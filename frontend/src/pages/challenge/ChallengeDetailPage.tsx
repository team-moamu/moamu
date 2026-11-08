import { useEffect, useState } from 'react'
import { Link, useOutletContext, useParams } from 'react-router'
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
  const [canViewCompletion, setCanViewCompletion] = useState(false)

  useEffect(() => {
    let cancelled = false

    async function fetchChallengeDetail() {
      setLoading(true)
      setError(null)
      setDetails([])
      setCanViewCompletion(false)
      setPageTitle('')

      try {
        const id = Number(challengeId)

        if (!challengeId || !Number.isSafeInteger(id) || id <= 0) {
          throw new Error('올바르지 않은 도전 ID입니다.')
        }

        const data = await getChallengeDetail(id)

        if (!cancelled) {
          setDetails(data)
          // 목록 데이터로는 진입 후보만 판단. 실제 완료 확정 여부는 완료 API에서 다시 검증
          const today = new Intl.DateTimeFormat('sv-SE', {
            timeZone: 'Asia/Seoul', year: 'numeric', month: '2-digit', day: '2-digit',
          }).format(new Date())
          setCanViewCompletion(data.length > 0 && data.every(detail =>
            detail.scheduleDate.slice(0, 10) < today && ['SUCCESS', 'FAILED'].includes(detail.scheduleStatus)))
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
      {canViewCompletion && (
        <Link to={`/challenge/${challengeId}/completion`}
          className="rounded-md bg-[var(--color-primary)] px-5 py-3 text-center text-white">
          도전 완료 결과 보기
        </Link>
      )}
    </div>
  )
}
