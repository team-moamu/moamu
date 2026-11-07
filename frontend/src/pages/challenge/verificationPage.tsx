
import { useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router'

import { Text } from '../../components/atoms/typography/Text'
import { SubText } from '../../components/atoms/typography/SubText'
import { SuccessButton } from '../../components/atoms/button/successButton'
import { Icon } from '../../components/atoms/icon/Icon'
import { VerificationImage } from '../../components/molecules/form/VerificationImage'
import { StatusMessage } from '../../components/molecules/feedback/StatusMessage'
import { getChallengeVerification } from '../../api/ChallengeApi'
import { uploadVerificationImage } from '../../api/MediaApi'
import type { ChallengeVerificationResponse } from '../../types/Challenge'

const MAX_ATTEMPTS = 3

export function VerificationPage() {
  const { challengeId } = useParams()
  const [searchParams] = useSearchParams()
  const scheduleId = searchParams.get('scheduleId')
  const [image, setImage] = useState<File | null>(null)
  const [mediaId, setMediaId] = useState<number | null>(null)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const uploadController = useRef<AbortController | null>(null)

  useEffect(() => {
    return () => uploadController.current?.abort()
  }, [])

  const handleImageChange = async (file: File | null) => {
    uploadController.current?.abort()
    setImage(file)
    setMediaId(null)
    setUploadError(null)
    setUploading(false)
    if (!file) return

    const controller = new AbortController()
    uploadController.current = controller
    setUploading(true)
    try {
      const result = await uploadVerificationImage(file, controller.signal)
      if (!controller.signal.aborted) {
        setMediaId(result.mediaId)
      }
    } catch (err) {
      if (!controller.signal.aborted) {
        setUploadError(err instanceof Error ? err.message : '사진 업로드에 실패했습니다.')
      }
    } finally {
      if (!controller.signal.aborted) {
        setUploading(false)
      }
    }
  }

  const [challenge, setChallenge] = useState<ChallengeVerificationResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    uploadController.current?.abort()
    setImage(null)
    setMediaId(null)
    setUploadError(null)
    setUploading(false)

    async function fetchVerification() {
      setLoading(true)
      setError(null)
      setChallenge(null)

      try {
        const id = Number(challengeId)
        const roundId = Number(scheduleId)
        if (!challengeId || !scheduleId ||
            !Number.isSafeInteger(id) || id <= 0 ||
            !Number.isSafeInteger(roundId) || roundId <= 0) {
          throw new Error('올바르지 않은 도전 또는 회차 ID입니다.')
        }

        const data = await getChallengeVerification(id, roundId, controller.signal)
        if (!controller.signal.aborted) {
          setChallenge(data)
        }
      } catch (err) {
        if (!controller.signal.aborted) {
          setError(err instanceof Error ? err.message : '인증 정보를 불러오지 못했습니다.')
        }
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false)
        }
      }
    }

    fetchVerification()
    return () => controller.abort()
  }, [challengeId, scheduleId])

  const handleVerification = () => {
    if (!image) {
      alert('인증 사진을 등록해주세요.')
      return
    }
    if (uploading || mediaId === null) return

    // TODO: 인증 API 호출
    console.log('challengeId:', challengeId)
    console.log('scheduleId:', scheduleId)
    console.log('image:', image)
    console.log('mediaId:', mediaId)
  }

  if (loading || error || !challenge) {
    return (
      <div className="p-5">
        <StatusMessage message={loading ? '인증 정보를 불러오는 중입니다.'
          : error ?? '해당 회차의 인증 정보가 없습니다.'} />
      </div>
    )
  }

  const remainingAttempts = Math.max(0, MAX_ATTEMPTS - challenge.attemptCount)

  return (
    <div className="flex h-full min-h-0 flex-col bg-white px-5 pt-6">
      {/* 상단 콘텐츠 */}
      <div className="flex flex-col gap-3">
        {/* 도전 정보 */}
        <div className="flex items-center justify-between">
          <Text>{challenge.challengeTitle}</Text>

          <SubText>
            Day {challenge.dayNo} / {challenge.dayCount}
            {' · '}남은 시도 {remainingAttempts}/
            {MAX_ATTEMPTS}
          </SubText>
        </div>

        {/* 인증 사진 */}
        <VerificationImage key={`${challengeId}-${scheduleId}`} onImageChange={handleImageChange} />
        {uploading && <StatusMessage message="사진을 업로드하는 중입니다." />}
        {uploadError && (
          <div className="flex flex-col gap-2">
            <StatusMessage message={uploadError} />
            <SuccessButton onClick={() => handleImageChange(image)}>
              업로드 다시 시도
            </SuccessButton>
          </div>
        )}
        {mediaId !== null && <StatusMessage message="사진 업로드가 완료되었습니다." />}

        {/* 인증 필수 조건 */}
        <section className="rounded-md border border-[#E4C9A4] bg-[#FFF8ED] p-4 shadow-sm">
          <h3 className="mb-3 text-base font-bold text-[#4B3828]">
            인증 필수 조건
          </h3>

          <div className="flex flex-col gap-2">
            {challenge.description.length === 0 && (
              <SubText>등록된 인증 조건이 없습니다.</SubText>
            )}
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
            {remainingAttempts} / {MAX_ATTEMPTS}
          </SubText>
        </section>
      </div>

      {/* 하단 인증 버튼 */}
      <div className="mt-auto pb-2 pt-6">
        <SuccessButton onClick={handleVerification} disabled={uploading || mediaId === null}>
          인증하기
        </SuccessButton>
      </div>
    </div>
  )
}
