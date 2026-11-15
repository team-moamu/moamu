import { useEffect, useId, useRef, type ReactNode } from 'react'
import { Icon, type IconName } from '../../atoms/icon/Icon'
import { DialogButton } from '../../atoms/button/DialogButton'
import { DialogTitle } from '../../atoms/typography/DialogTitle'
import { DialogDescription } from '../../atoms/typography/DialogDescription'

interface ConfirmDialogProps {
  title: string
  description?: ReactNode
  confirmLabel?: string
  cancelLabel?: string
  icon?: IconName
  onCancel: () => void
  onConfirm?: () => void
  pending?: boolean
  error?: string | null
  variant?: 'confirm' | 'alert'
}

// 부모에서 조건부 렌더링으로 열고 닫습니다.
export function ConfirmDialog({ title, description, confirmLabel = '확인', cancelLabel = '취소', icon = 'alert', onCancel, onConfirm, pending = false, error, variant = 'confirm' }: ConfirmDialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const descriptionId = useId()

  useEffect(() => {
    const dialog = dialogRef.current
    const previousFocus = document.activeElement
    const previousOverflow = document.body.style.overflow
    dialog?.showModal()
    document.body.style.overflow = 'hidden'
    return () => {
      dialog?.close()
      document.body.style.overflow = previousOverflow
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus()
    }
  }, [])

  return (
    <dialog
      ref={dialogRef}
      role={variant === 'alert' ? 'alertdialog' : 'dialog'}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      aria-busy={pending}
      onCancel={(event) => {
        event.preventDefault()
        if (!pending) onCancel()
      }}
      onClick={(event) => {
        if (pending || event.target !== event.currentTarget) return
        const bounds = event.currentTarget.getBoundingClientRect()
        if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) onCancel()
      }}
      className="fixed inset-x-0 top-auto bottom-0 mx-auto my-0 max-h-[90dvh] w-full max-w-[430px] overflow-y-auto rounded-t-[28px] border-0 bg-white px-6 pt-7 pb-[calc(24px+env(safe-area-inset-bottom))] text-center shadow-[0_-4px_20px_rgb(0_0_0/12%)] backdrop:bg-black/20"
    >
      <div className="mb-4 flex justify-center text-[#493725]"><Icon name={icon} size={40} /></div>
      <DialogTitle id={titleId}>{title}</DialogTitle>
      {description && <DialogDescription id={descriptionId} className="mt-2">{description}</DialogDescription>}
      {error && <p role="alert" className="mt-3 text-sm text-[var(--color-error)]">{error}</p>}
      <div className="mt-6 flex gap-3">
        {variant === 'confirm' && <DialogButton variant="cancel" onClick={onCancel} disabled={pending} autoFocus>{cancelLabel}</DialogButton>}
        <DialogButton onClick={onConfirm} disabled={pending || !onConfirm} autoFocus={variant === 'alert'}>{confirmLabel}</DialogButton>
      </div>
    </dialog>
  )
}
