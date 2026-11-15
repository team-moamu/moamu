import type { InputHTMLAttributes } from 'react'
import { Icon } from '../icon/Icon'

type SearchInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, 'type'>

export function SearchInput({
  placeholder = '작품 이름을 검색해 보세요.',
  className = '',
  'aria-label': ariaLabel = '작품 이름 검색',
  ...props
}: SearchInputProps) {
  return (
    <div className="relative w-[293px] shrink-0 text-[#4A382A]">
      <Icon
        name="search"
        size={20}
        className="pointer-events-none absolute top-1/2 left-3 -translate-y-1/2"
      />
      <input
        {...props}
        type="search"
        aria-label={ariaLabel}
        placeholder={placeholder}
        className={`h-[32px] w-full rounded-[8px] border border-[#8A7762] bg-transparent pr-3 pl-10 font-[family-name:var(--font-ui)] text-[12px] font-normal placeholder:text-[#8A7762] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#4F7652] disabled:cursor-not-allowed disabled:opacity-50 ${className}`}
      />
    </div>
  )
}
