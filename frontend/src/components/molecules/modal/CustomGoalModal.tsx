import { useEffect, useId, useRef, useState } from 'react'
import { CancelButton } from '../../atoms/button/CancelButton'
import { SuccessButton } from '../../atoms/button/successButton'
import { TextInput } from '../../atoms/input/TextInput'
import { Text } from '../../atoms/typography/Text'
import { Description } from '../../atoms/typography/Description'
import { FormLabel } from '../../atoms/typography/FormLabel'
import { SubText } from '../../atoms/typography/SubText'

interface CustomGoalModalProps {
  value: string
  onClose: () => void
  onConfirm: (goal: string) => void
}

export function CustomGoalModal({ value, onClose, onConfirm }: CustomGoalModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const id = useId()
  const [goal, setGoal] = useState(value)
  const canConfirm = goal.trim().length >= 5 && goal.length <= 20

  useEffect(() => {
    const dialog = dialogRef.current
    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    dialog?.showModal()

    return () => {
      dialog?.close()
      document.body.style.overflow = previousOverflow
    }
  }, [])

  return (
    <dialog
      ref={dialogRef}
      onCancel={onClose}
      onClick={(event) => {
        if (event.target !== event.currentTarget) return
        const bounds = event.currentTarget.getBoundingClientRect()
        const isOutside = event.clientX < bounds.left || event.clientX > bounds.right
          || event.clientY < bounds.top || event.clientY > bounds.bottom
        if (isOutside) onClose()
      }}
      aria-labelledby={`${id}-title`}
      aria-describedby={`${id}-description`}
      className="fixed inset-x-0 top-auto bottom-0 mx-auto mb-0 max-h-[90dvh] w-full max-w-[430px] overflow-y-auto rounded-t-[32px] border-0 bg-white px-6 pt-2 pb-[max(24px,env(safe-area-inset-bottom))] text-[#4B3A29] backdrop:bg-[#4B3A29]/30"
    >
      <div aria-hidden="true" className="mx-auto mb-4 h-1 w-16 rounded-full bg-[#F4E7D4]" />
      <h2 id={`${id}-title`}>
        <Text className="text-description--header">나만의 도전</Text>
      </h2>
      <Description id={`${id}-description`} className="mt-1">
        매일 사진으로 인증할 수 있는 목표를 적어 주세요.
      </Description>

      <form
        className="mt-3"
        onSubmit={(event) => {
          event.preventDefault()
          if (canConfirm) onConfirm(goal.trim())
        }}
      >
        <FormLabel htmlFor={`${id}-goal`} className="mb-2 block">목표 도전</FormLabel>
        <TextInput
          multiline
          id={`${id}-goal`}
          value={goal}
          onChange={setGoal}
          maxLength={20}
          placeholder="매일 퇴근 후 기타 30분씩 치기"
          describedBy={`${id}-hint ${id}-count`}
        />
        <div className="mt-1.5 flex justify-between text-sm">
          <span id={`${id}-hint`}><SubText>5~20자로 적어 주세요</SubText></span>
          <span id={`${id}-count`}><SubText>{goal.length} / 20</SubText></span>
        </div>
        <Description className="mt-2 flex items-center gap-2 rounded-md bg-[#FFF8ED] px-3 py-4">
          <span aria-hidden="true" className="icon-[lucide--circle-alert] inline-block size-5 shrink-0" />
          입력한 목표 문장은 인증 조건을 만들기 위해 AI로 전달돼요.
        </Description>

        <div className="mt-14 grid grid-cols-2 gap-4">
          <CancelButton onClick={onClose} />
          <SuccessButton
            type="submit"
            disabled={!canConfirm}
          >
            확인
          </SuccessButton>
        </div>
      </form>
    </dialog>
  )
}
