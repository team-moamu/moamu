import { Avatar } from "../../atoms/avatar/Avatar";

// 메인에서 사용하는 헤더
export function MainHeader() {
  return (
    <header className="flex w-full items-center justify-between bg-[var(--color-background)] px-6 py-4">
      <span className="text-accent text-[28px]">MOAMU</span>
      <Avatar />
    </header>
  )
}