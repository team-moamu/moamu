import { NavLink } from 'react-router'
import { Icon } from '../../atoms/icon/Icon' 
import type { IconName } from '../../atoms/icon/Icon'

const navItems: {
  label: string
  path: string
  icon: IconName
}[] = [
  { label: '홈', path: '/', icon: 'home' },
  { label: '도전하기', path: '/challenge', icon: 'challenge' },
  { label: '미술관', path: '/museum', icon: 'museum' },
  { label: '보관함', path: '/collection', icon: 'collection' },
  { label: '마이', path: '/mypage', icon: 'mypage' },
]

export function BottomNavigation() {
  return (
    <nav className="fixed bottom-0 left-0 z-50 flex w-full items-center justify-around border-t border-[var(--color-border)] bg-white px-3 py-4">
      {navItems.map((item) => (
        <NavLink
          key={item.path}
          to={item.path}
          end={item.path === '/'}
          className={({ isActive }) =>
            `flex flex-1 flex-col items-center justify-center gap-2 ${
              isActive
                ? 'text-[var(--color-primary)]'
                : 'text-[var(--color-text)]'
            }`
          }
        >
          <Icon name={item.icon} size={32} />

          <span className="text-label">
            {item.label}
          </span>
        </NavLink>
      ))}
    </nav>
  )
}