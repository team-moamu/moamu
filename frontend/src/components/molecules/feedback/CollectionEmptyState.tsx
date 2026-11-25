import { Link } from 'react-router'
import emptyMu from '../../../assets/images/drawing_mu.png'
import sadMu from '../../../assets/images/sad_mu.png'

type CollectionEmptyStateProps =
  | { variant: 'empty'; startTo?: string }
  | { variant: 'error'; onRetry: () => void }

export function CollectionEmptyState(props: CollectionEmptyStateProps) {
  const isError = props.variant === 'error'
  const actionClass = 'flex min-h-12 w-full cursor-pointer items-center justify-center rounded-[10px] bg-[var(--color-primary)] px-4 py-3 text-sm text-white hover:bg-[var(--color-primary-hover)]'

  return (
    <div className="flex min-h-[340px] flex-col items-center pt-8 pb-2 text-center">
      <div role={isError ? 'alert' : 'status'} className="flex flex-1 flex-col items-center justify-center">
        <img src={isError ? sadMu : emptyMu} alt="" className="h-[180px] w-[200px] object-contain" />
        {isError ? (
          <div className="mt-4 text-[var(--color-text)]">
            <p className="m-0 text-base font-semibold">작품을 불러오지 못했어요.</p>
            <p className="mt-2 text-sm">잠시 후 다시 시도해 주세요.</p>
          </div>
        ) : (
          <p className="mt-4 text-sm text-[var(--color-text)]">첫 작품을 위한 자리가 준비됐어요.</p>
        )}
      </div>
      <div className="mt-8 w-full">
        {isError ? (
          <button type="button" onClick={props.onRetry} className={actionClass}>다시 불러오기</button>
        ) : (
          <Link to={props.startTo ?? '/challenge/list'} className={actionClass}>첫 도전 시작하기</Link>
        )}
      </div>
    </div>
  )
}
