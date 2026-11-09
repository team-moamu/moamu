import { useState } from 'react'
import { HomeGallery } from '../../components/molecules/card/HomeBanner'
import { Navigate } from 'react-router'
import { HomeDateSelector } from '../../components/molecules/select/HomeDateSelector'
import { HomeChallengeSection } from '../../components/molecules/card/HomeChallengeSection'
import { dateKey, shiftDate } from './homeData'
import { useHomeChallenges } from './useHomeChallenges'

const greetingDateFormat = new Intl.DateTimeFormat('ko-KR', {
  month: 'long',
  day: 'numeric',
  weekday: 'long',
})

export function HomePage() {
  const [today] = useState(() => shiftDate(new Date(), 0))
  const [selectedDate, setSelectedDate] = useState(today)
  const { mode, challenges, dates } = useHomeChallenges(selectedDate)
  const isToday = dateKey(selectedDate) === dateKey(today)

  function selectDate(date: Date) {
    if (dateKey(date) !== dateKey(selectedDate)) {
      setSelectedDate(date)
    }
  }

  if (mode === 'unauthenticated') {
    return <Navigate to="/login" replace />
  }

  return (
    <div className="min-h-[calc(100dvh-150px)] bg-white px-3.5 pt-3 pb-[18px]">
      <div className="px-2 pb-3">
        <p className="m-0 mb-[3px] text-sm">{greetingDateFormat.format(today)}</p>
        <h1 className="m-0 font-[family-name:var(--font-description)] text-[19px] font-bold">
          매일의 기록이 작품이 돼요.
        </h1>
      </div>

      {mode === 'error' && (
        <p role="alert" className="px-2 py-3 text-sm">
          도전 목록을 불러오지 못했어요. 잠시 후 다시 시도해 주세요.
        </p>
      )}
      <HomeGallery challenges={challenges} mode={mode} isToday={isToday} />
      <HomeDateSelector dates={dates} selectedDate={selectedDate} onSelect={selectDate} />
      <HomeChallengeSection challenges={challenges} mode={mode} isToday={isToday} />
    </div>
  )
}
