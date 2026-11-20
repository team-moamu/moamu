import { Avatar } from '../../atoms/avatar/Avatar'

type MainHeaderProps = {
  profilePhotoUrl?: string | null
}

export function MainHeader({ profilePhotoUrl }: MainHeaderProps) {
  return (
    <header className="flex w-full items-center justify-between bg-[var(--color-background)] px-6 py-4">
      <span className="text-accent text-[28px]">MOAMU</span>
      <Avatar profilePhotoUrl={profilePhotoUrl} />
    </header>
  )
}