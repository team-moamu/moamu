
import { useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router'

import { Text } from '../../components/atoms/typography/Text'
import { SubText } from '../../components/atoms/typography/SubText'
import { SuccessButton } from '../../components/atoms/button/successButton'
import { Icon } from '../../components/atoms/icon/Icon'
import { VerificationImage } from '../../components/molecules/form/VerificationImage'
import { StatusMessage } from '../../components/molecules/feedback/StatusMessage'
import { VerificationLoading } from '../../components/molecules/feedback/VerificationLoading'
import { getChallengeVerification, submitVerification } from '../../api/ChallengeApi'
import { uploadVerificationImage } from '../../api/MediaApi'
import type { ChallengeVerificationResponse, VerificationSubmitResponse } from '../../types/Challenge'

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
  const submitController = useRef<AbortController | null>(null)
  const submissionKey = useRef<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [result, setResult] = useState<VerificationSubmitResponse | null>(null)

  useEffect(() => {
    return () => {
      uploadController.current?.abort()
      submitController.current?.abort()
    }
  }, [])

  const handleImageChange = async (file: File | null) => {
    if (submitController.current) return
    submissionKey.current = null
    setSubmitError(null)
    setResult(null)
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
    submitController.current?.abort()
    submitController.current = null
    submissionKey.current = null
    uploadController.current?.abort()
    async function fetchVerification() {
      setImage(null)
      setMediaId(null)
      setUploadError(null)
      setUploading(false)
      setSubmitting(false)
      setSubmitError(null)
      setResult(null)
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

  const handleVerification = async () => {
    if (!image) {
      alert('인증 사진을 등록해주세요.')
      return
    }
    if (uploading || mediaId === null || !challenge || submitController.current) return
    const controller = new AbortController()
    submitController.current = controller
    submissionKey.current ??= crypto.randomUUID()
    setSubmitting(true)
    setSubmitError(null)
    try {
      const response = await submitVerification({
        challengeId: challenge.challengeId,
        scheduleId: challenge.scheduleId,
        mediaId,
        idempotencyKey: submissionKey.current,
      }, controller.signal)
      if (!controller.signal.aborted) {
        setResult(response)
        setChallenge(current => current ? { ...current, attemptCount: response.attemptCount } : current)
      }
    } catch (err) {
      if (!controller.signal.aborted) {
        setSubmitError(err instanceof Error ? err.message : '인증 요청에 실패했습니다.')
      }
    } finally {
      if (!controller.signal.aborted) {
        submitController.current = null
        setSubmitting(false)
      }
    }
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
    <div className="relative flex h-full min-h-0 flex-col bg-white px-5 pt-6" aria-busy={submitting}>
      {submitting && (
        <VerificationLoading challengeTitle={challenge.challengeTitle} dayNo={challenge.dayNo} dayCount={challenge.dayCount} />
      )}
      <div className="contents" inert={submitting}>
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
        {submitError && <StatusMessage message={submitError} />}
        {result && (
          <section className="rounded-md border border-[#E4C9A4] bg-[#FFF8ED] p-4">
            <StatusMessage message={result.status === 'SUCCESS' ? '인증에 성공했어요!'
              : result.status === 'REVIEW_REQUIRED' ? '추가 검토가 필요한 사진이에요.'
              : result.status === 'TECH_ERROR' ? 'AI 판정 중 오류가 발생했어요. 다시 시도해 주세요.'
              : '인증 조건을 충족하지 못했어요.'} />
            {result.checks.map(check => <p key={check.code} className="mt-2 text-sm">{check.reason}</p>)}
          </section>
        )}
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
        <SuccessButton onClick={handleVerification} disabled={submitting || uploading || mediaId === null || (remainingAttempts === 0 && !submitError && result?.status !== 'TECH_ERROR')
          || result?.status === 'SUCCESS' || result?.status === 'REVIEW_REQUIRED' || result?.status === 'FAILED'}>
          인증하기
        </SuccessButton>
      </div>
      </div>
    </div>
  )
}
