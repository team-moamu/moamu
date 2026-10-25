// 이 파일은 폴더 구조 구성을 위한 임시 파일입니다. 첫 수정자는 이 주석을 삭제한 후 사용해 주세요.
import { Badge } from '../../atoms/badge/Badge'

type HabitCardProps = { title: string; completed?: boolean }

export function HabitCard({ title, completed = false }: HabitCardProps) {
  return <article><h3>{title}</h3><Badge>{completed ? '완료' : '진행 중'}</Badge></article>
}
