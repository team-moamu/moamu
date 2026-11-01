import type { HTMLAttributes } from 'react'

// // 사용 예시 : 확인했으면 없애셔도 됩니다!!
// <Icon name="home" size={28} />
// <Icon name="challenge" size={28} />
// <Icon name="back" size={20} />

export const iconMap = {
  home: 'icon-[lucide--house]',
  challenge: 'icon-[lucide--puzzle]',
  museum: 'icon-[lucide--landmark]',
  collection: 'icon-[lucide--archive]',
  mypage: 'icon-[lucide--user-round]',
  back: 'icon-[lucide--arrow-left]',

  // 목표 카드 아이콘
  study: 'icon-[lucide--book-open]',
  exercise: 'icon-[lucide--dumbbell]',
  water: 'icon-[lucide--droplet]',
  algorithm: 'icon-[lucide--pencil]',
  supplement: 'icon-[lucide--pill]',
  nametag: 'icon-[lucide--contact]',
} as const

export type IconName = keyof typeof iconMap

interface IconProps extends HTMLAttributes<HTMLSpanElement> {
  name: IconName
  size?: number
}

export function Icon({
  name,
  size = 24,
  className = '',
  style,
  ...props
}: IconProps) {
  return (
    <span
      {...props}
      aria-hidden="true"
      className={`${iconMap[name]} inline-block shrink-0 ${className}`}
      style={{ fontSize: size, ...style }}
    />
  )
}
