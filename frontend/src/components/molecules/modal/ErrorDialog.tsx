import type { ReactNode } from 'react'
import { ConfirmDialog } from './ConfirmDialog'

interface ErrorDialogProps {
  title: string
  description: ReactNode
  onClose: () => void
}

export function ErrorDialog({ title, description, onClose }: ErrorDialogProps) {
  return (
    <ConfirmDialog
      variant="alert"
      title={title}
      description={description}
      confirmLabel="확인"
      onConfirm={onClose}
      onCancel={onClose}
    />
  )
}
