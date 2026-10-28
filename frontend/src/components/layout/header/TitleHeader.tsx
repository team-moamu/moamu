// TitleHeader.tsx
interface TitleHeaderProps {
  title: string
}

export function TitleHeader({ title }: TitleHeaderProps) {
  return (
    <header className="header">
      <h1 className="text-description">{title}</h1>
    </header>
  )
}