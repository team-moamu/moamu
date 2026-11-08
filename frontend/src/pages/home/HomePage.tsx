import { useState } from 'react'
import { HomeGallery } from './components/HomeGallery'
import { HomeDateSelector } from './components/HomeDateSelector'
import { HomeChallengeSection } from './components/HomeChallengeSection'
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
  const { mode, challenges, dates } = useHomeChallenges(selectedDate, today)
  const isToday = dateKey(selectedDate) === dateKey(today)

  function selectDate(date: Date) {
    if (dateKey(date) !== dateKey(selectedDate)) {
      setSelectedDate(date)
    }
  }

  return (
    <div className="min-h-[calc(100dvh-150px)] bg-white px-3.5 pt-3 pb-[18px]">
      <div className="px-2 pb-3">
        <p className="m-0 mb-[3px] text-sm">{greetingDateFormat.format(today)}</p>
        <h1 className="m-0 font-[family-name:var(--font-description)] text-[19px] font-bold">
          매일의 기록이 작품이 돼요.
        </h1>
      </div>

      <HomeGallery challenges={challenges} mode={mode} isToday={isToday} />
      <HomeDateSelector dates={dates} selectedDate={selectedDate} onSelect={selectDate} />
      <HomeChallengeSection challenges={challenges} mode={mode} isToday={isToday} />
    </div>
  )
}
