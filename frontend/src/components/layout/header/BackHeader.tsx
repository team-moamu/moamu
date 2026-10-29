
// 뒤로가기가 있는 헤더
import { useNavigate } from 'react-router'
interface BackHeaderProps {
  title: string
}

export function BackHeader({ title }: BackHeaderProps) {
  const navigate = useNavigate()

  return (
    <header className="relative flex w-full items-center justify-center px-6 py-4">
      <button
        type="button"
        onClick={() => navigate(-1)}
        className="absolute left-6 flex items-center justify-center"
        aria-label="뒤로가기"
      >
        <span
          aria-hidden="true"
          className="icon-[lucide--arrow-left] inline-block shrink-0 text-xl"
        />
      </button>

      <h1 className="text-description m-0">
        {title}
      </h1>
    </header>
  )
}