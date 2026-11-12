import type { Ref } from 'react'

// 제목만 있는 헤더
interface TitleHeaderProps {
  title: string
  headingRef?: Ref<HTMLHeadingElement>
}

export function TitleHeader({ title, headingRef }: TitleHeaderProps) {
  return (
    <header className="flex w-full items-center justify-center px-6 py-4">
      <h1 className="text-description text-description--header m-0 focus:outline-none" ref={headingRef} tabIndex={headingRef ? -1 : undefined}>
        {title}
      </h1>
    </header>
  )
}