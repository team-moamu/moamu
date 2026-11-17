type PuzzleCardProps = { title: string }

export function PuzzleCard({ title }: PuzzleCardProps) {
  return <article><h3>{title}</h3></article>
}
