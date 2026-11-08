import { useEffect, useRef, useState } from 'react'
import { AllAgreeButton } from '../../atoms/button/AllAgreeButton'
import { AgreeContinueButton } from '../../atoms/button/AgreeContinueButton'
import { Input } from '../../atoms/input/Input'
import { Heading } from '../../atoms/typography/Heading'
import { Description } from '../../atoms/typography/Description'

interface TermsModalProps {
  onClose: () => void
  onAgree: (marketingAgreed: boolean) => void
}

export function TermsModal({ onClose, onAgree }: TermsModalProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)

  const [serviceAgreed, setServiceAgreed] = useState(false)
  const [privacyAgreed, setPrivacyAgreed] = useState(false)
  const [marketingAgreed, setMarketingAgreed] = useState(false)

  // 필수 약관을 모두 선택하면 다음 단계로 진행할 수 있습니다.
  const canContinue = serviceAgreed && privacyAgreed
  const allAgreed = canContinue && marketingAgreed

  // 컴포넌트가 표시되면 모달을 엽니다.
  useEffect(() => {
    const dialog = dialogRef.current
    dialog?.showModal()

    return () => dialog?.close()
  }, [])

  // 전체 동의 선택 시 개별 약관도 함께 변경합니다.
  function handleAllChange(checked: boolean) {
    setServiceAgreed(checked)
    setPrivacyAgreed(checked)
    setMarketingAgreed(checked)
  }

  return (
    <dialog
      ref={dialogRef}
      onCancel={onClose}
      onClick={(event) => {
        if (event.target !== event.currentTarget) return

        // 모달 내부 여백은 유지하고, 바깥 배경을 클릭했을 때만 닫습니다.
        const bounds = event.currentTarget.getBoundingClientRect()
        const isOutside = event.clientX < bounds.left || event.clientX > bounds.right
          || event.clientY < bounds.top || event.clientY > bounds.bottom
        if (isOutside) onClose()
      }}
      aria-labelledby="terms-title"
      className="
        fixed inset-0 m-auto
        w-[calc(100%-32px)] max-w-sm
        rounded-3xl border-0 bg-white p-6
        text-[#493725]
        backdrop:bg-black/40
      "
    >
      {/* 제목과 안내 */}
      <div id="terms-title">
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
        <label className="flex items-center gap-2 text-caption">
          <Input
            type="checkbox"
            checked={serviceAgreed}
            onChange={(event) => setServiceAgreed(event.target.checked)}
            className="accent-[#426B45]"
          />
          [필수] 서비스 이용약관
        </label>

        <label className="flex items-center gap-2 text-caption">
          <Input
            type="checkbox"
            checked={privacyAgreed}
            onChange={(event) => setPrivacyAgreed(event.target.checked)}
            className="accent-[#426B45]"
          />
          [필수] 개인정보 수집·이용
        </label>

        <label className="flex items-center gap-2 text-caption">
          <Input
            type="checkbox"
            checked={marketingAgreed}
            onChange={(event) => setMarketingAgreed(event.target.checked)}
            className="accent-[#426B45]"
          />
          [선택] 마케팅 정보 수신
        </label>
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
    </dialog>
  )
}
