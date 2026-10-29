import { Avatar } from "../../atoms/avatar/Avatar";

// 메인에서 사용하는 헤더
export function MainHeader() {
  return (
    <header className="flex w-full items-center justify-between px-6 py-4">
      <span className="text-accent">MOAMU</span>
      <Avatar />
    </header>
  )
}