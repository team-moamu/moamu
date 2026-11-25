import { useEffect, useId, useRef, useState } from 'react'
import { dateKey } from '../../../pages/home/HomeData'
import { Icon } from '../../atoms/icon/Icon'

const weekdays = ['일', '월', '화', '수', '목', '금', '토']
const monthFormat = new Intl.DateTimeFormat('ko-KR', { year: 'numeric', month: 'long' })
const dateFormat = new Intl.DateTimeFormat('ko-KR', { dateStyle: 'full' })
const controlClassName = 'flex size-10 cursor-pointer items-center justify-center rounded-full text-[var(--color-primary)] hover:bg-[var(--color-border)]'

interface HomeCalendarDialogProps {
  selectedDate: Date
  onClose: () => void
  onSelect: (date: Date) => void
}

export function HomeCalendarDialog({ selectedDate, onClose, onSelect }: HomeCalendarDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const [month, setMonth] = useState(() => new Date(
    selectedDate.getFullYear(), selectedDate.getMonth(), 1, 12,
  ))
  const year = month.getFullYear()
  const monthIndex = month.getMonth()
  const daysInMonth = new Date(year, monthIndex + 1, 0, 12).getDate()
  const selectedKey = dateKey(selectedDate)
  const [todayKey] = useState(() => dateKey(new Date()))

  useEffect(() => {
    const dialog = dialogRef.current
    const previousFocus = document.activeElement
    const previousOverflow = document.body.style.overflow
    dialog?.showModal()
    document.body.style.overflow = 'hidden'
    return () => {
      dialog?.close()
      document.body.style.overflow = previousOverflow
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus()
    }
  }, [])

  function moveMonth(offset: number) {
    setMonth(current => new Date(current.getFullYear(), current.getMonth() + offset, 1, 12))
  }

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby={titleId}
      onCancel={event => {
        event.preventDefault()
        onClose()
      }}
      onClick={event => {
        if (event.target !== event.currentTarget) return
        const bounds = event.currentTarget.getBoundingClientRect()
        if (
          event.clientX < bounds.left || event.clientX > bounds.right ||
          event.clientY < bounds.top || event.clientY > bounds.bottom
        ) {
          onClose()
        }
      }}
      className="fixed inset-0 m-auto max-h-[90dvh] w-[calc(100%_-_2rem)] max-w-[380px] overflow-y-auto rounded-[24px] border border-[var(--color-border)] bg-white p-4 text-[var(--color-text)] shadow-lg backdrop:bg-black/25"
    >
      <h2 id={titleId} className="m-0 py-2 text-center font-[family-name:var(--font-description)] text-[19px] font-bold">달력</h2>
      <div className="mb-2 flex items-center justify-between">
        <button type="button" aria-label="이전 달" className={controlClassName} onClick={() => moveMonth(-1)}>
          <Icon name="chevronRight" size={20} className="rotate-180" />
        </button>
        <h3 aria-live="polite" className="m-0 text-base font-semibold">{monthFormat.format(month)}</h3>
        <button type="button" aria-label="다음 달" className={controlClassName} onClick={() => moveMonth(1)}>
          <Icon name="chevronRight" size={20} />
        </button>
      </div>
      <div className="grid grid-cols-7 gap-1 text-center">
        {weekdays.map(weekday => (
          <span key={weekday} aria-hidden="true" className="py-2 text-xs text-[var(--color-text-muted)]">{weekday}</span>
        ))}
        {Array.from({ length: month.getDay() }, (_, index) => <span key={index} aria-hidden="true" />)}
        {Array.from({ length: daysInMonth }, (_, index) => {
          const date = new Date(year, monthIndex, index + 1, 12)
          const key = dateKey(date)
          const isSelected = key === selectedKey

          return (
            <button
              key={key}
              type="button"
              aria-label={dateFormat.format(date)}
              aria-pressed={isSelected}
              aria-current={key === todayKey ? 'date' : undefined}
              autoFocus={isSelected}
              onClick={() => onSelect(date)}
              className={`min-h-10 cursor-pointer rounded-full text-sm ${isSelected
                ? 'bg-[var(--color-primary)] font-semibold text-white'
                : 'hover:bg-[var(--color-border)] aria-current:font-bold aria-current:text-[var(--color-primary)]'}`}
            >
              {date.getDate()}
            </button>
          )
        })}
      </div>
    </dialog>
  )
}
