// 제목만 있는 헤더
interface TitleHeaderProps {
  title: string
}

export function TitleHeader({ title }: TitleHeaderProps) {
  return (
    <header className="flex w-full items-center justify-center px-6 py-4">
      <h1 className="text-description text-description--header m-0">
        {title}
      </h1>
    </header>
  )
}