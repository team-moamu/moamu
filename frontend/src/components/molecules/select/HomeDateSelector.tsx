import { useState } from 'react'
import { dateKey } from '../../../pages/home/HomeData'
import { Icon } from '../../atoms/icon/Icon'
import { HomeCalendarDialog } from './HomeCalendarDialog'

const weekdays = ['일', '월', '화', '수', '목', '금', '토']

interface HomeDateSelectorProps {
  dates: { date: Date; hasRecord: boolean }[]
  selectedDate: Date
  onSelect: (date: Date) => void
}

export function HomeDateSelector({ dates, selectedDate, onSelect }: HomeDateSelectorProps) {
  const [calendarOpen, setCalendarOpen] = useState(false)

  return (
    <section className="mt-[19px]" aria-labelledby="week-heading">
      <div className="mx-2 mb-2.5 flex items-center justify-between">
        <h2
          id="week-heading"
          className="m-0 font-[family-name:var(--font-description)] text-[19px] font-bold"
        >
          이번 주의 기록
        </h2>
        <button
          type="button"
          aria-haspopup="dialog"
          onClick={() => setCalendarOpen(true)}
          className="flex min-h-9 cursor-pointer items-center gap-1.5 border-0 bg-transparent p-0 text-xs text-[var(--color-primary)]"
        >
          <Icon name="calendar" size={15} />
          달력
        </button>
      </div>
      {calendarOpen && (
        <HomeCalendarDialog
          selectedDate={selectedDate}
          onClose={() => setCalendarOpen(false)}
          onSelect={date => {
            onSelect(date)
            setCalendarOpen(false)
          }}
        />
      )}
      <div className="grid grid-cols-7 gap-[7px] max-[350px]:gap-1" aria-label="날짜 선택">
        {dates.map(({ date, hasRecord }) => (
          <DateCapsule
            key={dateKey(date)}
            date={date}
            isSelected={dateKey(date) === dateKey(selectedDate)}
            hasRecord={hasRecord}
            onSelect={onSelect}
          />
        ))}
      </div>
    </section>
  )
}

interface DateCapsuleProps {
  date: Date
  isSelected: boolean
  hasRecord: boolean
  onSelect: (date: Date) => void
}

function DateCapsule({ date, isSelected, hasRecord, onSelect }: DateCapsuleProps) {
  const weekday = weekdays[date.getDay()]
  const appearance = isSelected
    ? 'border-[var(--color-primary)] bg-[var(--color-primary)] text-white'
    : 'border-[var(--color-border)] bg-white text-[#181818]'

  return (
    <button
      type="button"
      aria-label={`${dateKey(date)} ${weekday}요일`}
      aria-pressed={isSelected}
      onClick={() => onSelect(date)}
      className={`flex h-[82px] min-w-0 cursor-pointer flex-col items-center justify-center
        gap-[5px] rounded-[30px] border-2 pt-2 pb-1.5 ${appearance}`}
    >
      <span className="text-xs">{weekday}</span>
      <strong className="text-[17px] font-medium">{date.getDate()}</strong>
      <span aria-hidden="true" className="flex size-1.5">
        {hasRecord && (
          <span
            className={`size-1.5 rounded-full ${isSelected ? 'bg-[#e9b964]' : 'bg-[var(--color-primary)]'}`}
          />
        )}
      </span>
    </button>
  )
}
