import { useEffect, useRef, useState } from 'react'
import { AllAgreeButton } from '../../atoms/button/AllAgreeButton'
import { AgreeContinueButton } from '../../atoms/button/AgreeContinueButton'
import { Icon } from '../../atoms/icon/Icon'
import { Input } from '../../atoms/input/Input'
import { Heading } from '../../atoms/typography/Heading'
import { Description } from '../../atoms/typography/Description'
import { signupTerms } from './signupTerms'
import { SignupTermsContent } from './SignupTermsContent'

interface TermsModalProps {
  onClose: () => void
  onAgree: (marketingAgreed: boolean) => void
}

export function TermsModal({ onClose, onAgree }: TermsModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const detailHeadingRef = useRef<HTMLHeadingElement>(null)
  const activeTriggerRef = useRef<HTMLButtonElement | null>(null)

  const [serviceAgreed, setServiceAgreed] = useState(false)
  const [privacyAgreed, setPrivacyAgreed] = useState(false)
  const [marketingAgreed, setMarketingAgreed] = useState(false)
  const [activeTerm, setActiveTerm] = useState<(typeof signupTerms)[number] | null>(null)

  // 필수 약관을 모두 선택하면 다음 단계로 진행할 수 있습니다.
  const canContinue = serviceAgreed && privacyAgreed
  const allAgreed = canContinue && marketingAgreed
  const agreements = [
    { checked: serviceAgreed, setChecked: setServiceAgreed },
    { checked: privacyAgreed, setChecked: setPrivacyAgreed },
    { checked: marketingAgreed, setChecked: setMarketingAgreed },
  ]

  // 컴포넌트가 표시되면 모달을 엽니다.
  useEffect(() => {
    const dialog = dialogRef.current
    dialog?.showModal()

    return () => dialog?.close()
  }, [])

  useEffect(() => {
    if (activeTerm) {
      detailHeadingRef.current?.focus()
    } else {
      activeTriggerRef.current?.focus()
      activeTriggerRef.current = null
    }
  }, [activeTerm])

  // 전체 동의 선택 시 개별 약관도 함께 변경합니다.
  function handleAllChange(checked: boolean) {
    setServiceAgreed(checked)
    setPrivacyAgreed(checked)
    setMarketingAgreed(checked)
  }

  return (
    <dialog
      ref={dialogRef}
      onCancel={(event) => {
        if (activeTerm) {
          event.preventDefault()
          setActiveTerm(null)
        } else {
          onClose()
        }
      }}
      onClick={(event) => {
        if (activeTerm || event.target !== event.currentTarget) return

        // 모달 내부 여백은 유지하고, 바깥 배경을 클릭했을 때만 닫습니다.
        const bounds = event.currentTarget.getBoundingClientRect()
        const isOutside = event.clientX < bounds.left || event.clientX > bounds.right
          || event.clientY < bounds.top || event.clientY > bounds.bottom
        if (isOutside) onClose()
      }}
      aria-labelledby="terms-title"
      className={`fixed inset-0 m-auto border-0 bg-white text-[#493725] backdrop:bg-black/40 ${
        activeTerm
          ? 'h-dvh max-h-none w-full max-w-[430px] p-0 min-[430px]:h-[min(90dvh,44rem)] min-[430px]:max-w-[398px] min-[430px]:rounded-3xl'
          : 'w-[calc(100%-32px)] max-w-sm rounded-3xl p-6'
      }`}
    >
      {activeTerm && (
        <div className="flex h-full flex-col">
          <h2 ref={detailHeadingRef} id="terms-title" tabIndex={-1} className="bg-[#FDF7EB] px-6 py-5 text-center text-lg font-bold outline-none">
            {activeTerm.title}
          </h2>
          <div className="flex min-h-0 flex-1 flex-col gap-4 px-6 py-6">
            <p id="term-content-label" className="text-caption">
              [{activeTerm.required ? '필수' : '선택'}] {activeTerm.title}
              {activeTerm.title === '개인정보 수집·이용' && ' 동의'}
            </p>
            <div role="region" aria-labelledby="term-content-label" tabIndex={0} className="min-h-0 min-w-0 flex-1 space-y-4 overflow-y-auto rounded-xl border border-[#EBD2B5] bg-[#FCF9F4] p-4 text-caption leading-relaxed">
              <SignupTermsContent title={activeTerm.title} />
            </div>
          </div>
          <div className="px-6 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
            <AgreeContinueButton disabled={false} onClick={() => setActiveTerm(null)}>
              확인하고 돌아가기
            </AgreeContinueButton>
          </div>
        </div>
      )}

      <div hidden={activeTerm !== null}>
        {/* 제목과 안내 */}
        <div id={activeTerm ? undefined : 'terms-title'}>
          <Heading>함께 시작하기 전에</Heading>
        </div>

        <Description className="mt-1">
          모아뮤 이용을 위한 약관을 확인해 주세요.
        </Description>

        {/* 전체 동의 */}
        <div className="mt-5">
          <AllAgreeButton checked={allAgreed} onChange={handleAllChange} />
        </div>

        {/* 개별 약관 */}
        <div className="mt-4 flex flex-col gap-4 border-t border-[#E5E5E5] pt-4">
          {signupTerms.map((term, index) => (
            <div key={term.title} className="flex items-center justify-between gap-2">
              <label className="flex items-center gap-2 text-caption">
                <Input
                  type="checkbox"
                  checked={agreements[index].checked}
                  onChange={(event) => agreements[index].setChecked(event.target.checked)}
                  className="accent-[#426B45]"
                />
                [{term.required ? '필수' : '선택'}] {term.title}
              </label>
              <button
                type="button"
                aria-label={`${term.title} 상세 보기`}
                className="-my-2 inline-flex size-9 shrink-0 cursor-pointer items-center justify-center rounded-md"
                onClick={(event) => {
                  activeTriggerRef.current = event.currentTarget
                  setActiveTerm(term)
                }}
              >
                <Icon name="chevronRight" size={16} />
              </button>
            </div>
          ))}
        </div>

        {/* 필수 동의 후 진행 */}
        <div className="mt-10">
          <AgreeContinueButton
            disabled={!canContinue}
            onClick={() => onAgree(marketingAgreed)}
          >
            동의하고 이메일 입력
          </AgreeContinueButton>
        </div>
      </div>
    </dialog>
  )
}
