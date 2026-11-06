
import { useState } from 'react'
import { useParams } from 'react-router'

import { Text } from '../../components/atoms/typography/Text'
import { SubText } from '../../components/atoms/typography/SubText'
import { SuccessButton } from '../../components/atoms/button/successButton'
import { Icon } from '../../components/atoms/icon/Icon'
import { VerificationImage } from '../../components/molecules/form/VerificationImage'

export function VerificationPage() {
  const { challengeId } = useParams()
  const [image, setImage] = useState<File | null>(null)

  // TODO: API 연동 후 변경
  const challenge = {
    challengeTitle: '명찰 챙기기',
    dayNo: 3,
    durationDays: 14,
    attemptCount: 1,
    maxAttempts: 3,
    description: [
      '명찰이 보여요',
      '명찰을 들고있는 손이 보여요',
    ],
  }

  const remainingAttempts =
    challenge.maxAttempts - challenge.attemptCount

  const handleVerification = () => {
    if (!image) {
      alert('인증 사진을 등록해주세요.')
      return
    }

    // TODO: 인증 API 호출
    console.log('challengeId:', challengeId)
    console.log('image:', image)
  }

  return (
    <div className="flex h-full min-h-0 flex-col bg-white px-5 pt-6">
      {/* 상단 콘텐츠 */}
      <div className="flex flex-col gap-3">
        {/* 도전 정보 */}
        <div className="flex items-center justify-between">
          <Text>{challenge.challengeTitle}</Text>

          <SubText>
            Day {challenge.dayNo} / {challenge.durationDays}
            {' · '}남은 시도 {remainingAttempts}/
            {challenge.maxAttempts}
          </SubText>
        </div>

        {/* 인증 사진 */}
        <VerificationImage onImageChange={setImage} />

        {/* 인증 필수 조건 */}
        <section className="rounded-md border border-[#E4C9A4] bg-[#FFF8ED] p-4 shadow-sm">
          <h3 className="mb-3 text-base font-bold text-[#4B3828]">
            인증 필수 조건
          </h3>

          <div className="flex flex-col gap-2">
            {challenge.description.map((description, index) => (
              <div
                key={index}
                className="flex items-center gap-2"
              >
                <Icon name="check" size={17} />
                <SubText>{description}</SubText>
              </div>
            ))}
          </div>
        </section>

        {/* 남은 시도 */}
        <section className="flex items-center justify-between rounded-md border border-[#E4C9A4] bg-[#FFF8ED] p-4 shadow-sm">
          <h3 className="text-base font-bold text-[#4B3828]">
            남은 시도
          </h3>

          <SubText>
            {remainingAttempts} / {challenge.maxAttempts}
          </SubText>
        </section>
      </div>

      {/* 하단 인증 버튼 */}
      <div className="mt-auto pb-2 pt-6">
        <SuccessButton onClick={handleVerification}>
          인증하기
        </SuccessButton>
      </div>
    </div>
  )
}
