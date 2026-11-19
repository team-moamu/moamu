import { useEffect, useId, useRef, useState } from 'react'
import { VerificationLoadingContent } from '../feedback/VerificationLoading'
import { CancelButton } from '../../atoms/button/CancelButton'
import { SuccessButton } from '../../atoms/button/SuccessButton'
import { TextInput } from '../../atoms/input/TextInput'
import { Text } from '../../atoms/typography/Text'
import { Description } from '../../atoms/typography/Description'
import { FormLabel } from '../../atoms/typography/FormLabel'
import { SubText } from '../../atoms/typography/SubText'

interface CustomGoalModalProps {
  value: string
  onClose: () => void
  onConfirm: (goal: string) => void
  isAnalyzing?: boolean
  error?: string
}

export function CustomGoalModal({ value, onClose, onConfirm, isAnalyzing = false, error }: CustomGoalModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const id = useId()
  const [goal, setGoal] = useState(value)
  const canConfirm = goal.trim().length > 0 && goal.length <= 500 && !isAnalyzing

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

  useEffect(() => {
    if (isAnalyzing) dialogRef.current?.focus()
  }, [isAnalyzing])

  return (
    <dialog
      ref={dialogRef}
      onCancel={(event) => {
        if (isAnalyzing) event.preventDefault()
        else onClose()
      }}
      onClick={(event) => {
        if (isAnalyzing || event.target !== event.currentTarget) return
        const bounds = event.currentTarget.getBoundingClientRect()
        const isOutside = event.clientX < bounds.left || event.clientX > bounds.right
          || event.clientY < bounds.top || event.clientY > bounds.bottom
        if (isOutside) onClose()
      }}
      aria-labelledby={isAnalyzing ? "verification-loading-title" : `${id}-title`}
      aria-describedby={isAnalyzing ? "verification-loading-description" : `${id}-description`}
      aria-busy={isAnalyzing}
      className={isAnalyzing ? "verification-loading verification-loading--dialog" : "fixed inset-x-0 top-auto bottom-0 mx-auto mb-0 max-h-[90dvh] w-full max-w-[430px] overflow-y-auto rounded-t-[32px] border-0 bg-white px-6 pt-2 pb-[max(24px,env(safe-area-inset-bottom))] text-[#4B3A29] backdrop:bg-[#4B3A29]/30"}
    >
      {isAnalyzing && <VerificationLoadingContent challengeTitle={goal.trim()} />}
      <div hidden={isAnalyzing}>
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
          <fieldset disabled={isAnalyzing} className="m-0 min-w-0 border-0 p-0">
            <FormLabel htmlFor={`${id}-goal`} className="mb-2 block">목표 도전</FormLabel>
            <TextInput
              multiline
              id={`${id}-goal`}
              value={goal}
              onChange={setGoal}
              maxLength={500}
              placeholder="매일 퇴근 후 기타 30분씩 치기"
              describedBy={`${id}-hint ${id}-count`}
            />
            <div className="mt-1.5 flex justify-between text-sm">
              <span id={`${id}-hint`}><SubText>1~500자로 적어 주세요</SubText></span>
              <span id={`${id}-count`}><SubText>{goal.length} / 500</SubText></span>
            </div>
            <Description className="mt-2 flex items-center gap-2 rounded-md bg-[#FFF8ED] px-3 py-4">
              <span aria-hidden="true" className="icon-[lucide--circle-alert] inline-block size-5 shrink-0" />
              입력한 목표 문장은 인증 조건을 만들기 위해 AI로 전달돼요.
            </Description>

            {error && <p role="alert" className="mt-3 text-sm text-[var(--color-error)]">{error}</p>}
            <div className="mt-14 grid grid-cols-2 gap-4">
              <CancelButton onClick={onClose} disabled={isAnalyzing} />
              <SuccessButton
                type="submit"
                disabled={!canConfirm}
              >
                {isAnalyzing ? '분석 중...' : 'AI 분석하기'}
              </SuccessButton>
            </div>
          </fieldset>
        </form>
      </div>
    </dialog>
  )
}
