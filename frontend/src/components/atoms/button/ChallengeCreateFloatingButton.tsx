import { Link } from 'react-router'
import { Icon } from '../icon/Icon'

export function ChallengeCreateFloatingButton() {
  return (
      <div className="pointer-events-none fixed bottom-28 left-1/2 z-40 flex w-full max-w-[430px] -translate-x-1/2 justify-end px-4">
        <Link
          to="/challenge/create"
          aria-label="도전 생성하기"
          title="도전 생성하기"
          className="pointer-events-auto flex h-12 w-12 items-center justify-center rounded-full border-2 border-white bg-[var(--color-primary)] text-white shadow-md ring-2 ring-[var(--color-primary)] transition-colors hover:bg-[var(--color-primary-hover)] focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-[var(--color-primary)] active:bg-[var(--color-primary-pressed)]"
        >
          <Icon name="palette" size={36} />
        </Link>
      </div>
  )
}
