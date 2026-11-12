// 이 파일은 폴더 구조 구성을 위한 임시 파일입니다. 첫 수정자는 이 주석을 삭제한 후 사용해 주세요.
import { Button } from '../../atoms/button/Button'

type ConfirmDialogProps = { message: string; onConfirm: () => void }

export function ConfirmDialog({ message, onConfirm }: ConfirmDialogProps) {
  return <div role="dialog" aria-label="확인"><p>{message}</p><Button onClick={onConfirm}>확인</Button></div>
}
