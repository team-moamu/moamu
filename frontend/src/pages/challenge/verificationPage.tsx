
import { useEffect, useRef, useState } from 'react'
import { useParams, useSearchParams } from 'react-router'

import { Text } from '../../components/atoms/typography/Text'
import { SubText } from '../../components/atoms/typography/SubText'
import { SuccessButton } from '../../components/atoms/button/successButton'
import { Icon } from '../../components/atoms/icon/Icon'
import { VerificationImage } from '../../components/molecules/form/VerificationImage'
import { StatusMessage } from '../../components/molecules/feedback/StatusMessage'
import { VerificationLoading } from '../../components/molecules/feedback/VerificationLoading'
import { getChallengeVerification } from '../../api/ChallengeApi'
import { MediaVerificationError, submitVerificationImage } from '../../api/MediaApi'
import type { ChallengeVerificationResponse, VerificationSubmitResponse } from '../../types/Challenge'

const MAX_ATTEMPTS = 3

type VerificationPageProps = {
  // 로그인 기능 연결 시 인증 상태에서 가져온 토큰 전달
  accessToken?: string
}

export function VerificationPage({ accessToken }: VerificationPageProps) {
  const { challengeId } = useParams()
  const [searchParams] = useSearchParams()
  const scheduleId = searchParams.get('scheduleId')
  const [image, setImage] = useState<File | null>(null)
  const submitController = useRef<AbortController | null>(null)
  const submissionKey = useRef<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [refreshError, setRefreshError] = useState<string | null>(null)
  const [result, setResult] = useState<VerificationSubmitResponse | null>(null)

  useEffect(() => {
    return () => {
      submitController.current?.abort()
    }
  }, [])

  // 사진 선택 시 미리보기만 변경. 서버 요청은 인증하기 버튼에서 실행
  const handleImageChange = (file: File | null) => {
    if (submitController.current) return
    submissionKey.current = null
    setSubmitError(null)
    setResult(null)
    setImage(file)
  }

  const [challenge, setChallenge] = useState<ChallengeVerificationResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const controller = new AbortController()
    submitController.current?.abort()
    submitController.current = null
    submissionKey.current = null
    async function fetchVerification() {
      // 도전 또는 회차가 바뀌면 이전 사진과 판정 상태 초기화
      setImage(null)
      setSubmitting(false)
      setSubmitError(null)
      setResult(null)
      setRefreshError(null)
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

        const data = await getChallengeVerification(id, roundId, controller.signal, accessToken)
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
  }, [challengeId, scheduleId, accessToken])

  // 사진 업로드 -> AI 판정 -> 결과 표시
  const handleVerification = async () => {
    if (!image) {
      alert('인증 사진을 등록해주세요.')
      return
    }
    // 진행 중인 요청이 있으면 중복 제출 방지
    if (!challenge || submitController.current) return
    const controller = new AbortController()
    submitController.current = controller
    // 응답 유실 후 재전송할 때 같은 인증으로 처리되도록 키 유지
    submissionKey.current ??= crypto.randomUUID()
    setSubmitting(true)
    setSubmitError(null)
    setRefreshError(null)
    try {
      // /internal/photo-verifications 호출은 /api/media의 백엔드 내부에서 처리
      const response = await submitVerificationImage(image, challenge.scheduleId,
        submissionKey.current, controller.signal, accessToken)
      if (!controller.signal.aborted) {
        setResult(response)
      }
    } catch (err) {
      if (!controller.signal.aborted) {
        setSubmitError(err instanceof Error ? err.message : '인증 요청에 실패했습니다.')
        // 기술 오류로 종료된 작업은 새 키로 제출. 단순 통신 오류는 기존 키 유지
        if (err instanceof MediaVerificationError && err.code === 'AI_TECHNICAL_ERROR') {
          submissionKey.current = null
        }
      }
    } finally {
      // 제출 응답에 시도 횟수가 없으므로 최신 회차 정보를 다시 조회
      if (!controller.signal.aborted) {
        try {
          const updated = await getChallengeVerification(challenge.challengeId, challenge.scheduleId,
            controller.signal, accessToken)
          if (!controller.signal.aborted) {
            if (updated) setChallenge(updated)
            else setRefreshError('회차 정보가 없습니다. 페이지를 새로고침해 주세요.')
          }
        } catch {
          if (!controller.signal.aborted) {
            setRefreshError('최신 시도 횟수를 확인하지 못했습니다. 페이지를 새로고침해 주세요.')
          }
        }
      }
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
        {refreshError && <StatusMessage message={refreshError} />}
        {result && (
          <section className="rounded-md border border-[#E4C9A4] bg-[#FFF8ED] p-4">
            <StatusMessage message={result.status === 'SUCCESS' ? '인증에 성공했어요!'
              : result.status === 'REVIEW_REQUIRED' ? '추가 검토가 필요한 사진이에요.'
              : '인증 조건을 충족하지 못했어요.'} />
            {result.checks.map(check => <p key={check.code} className="mt-2 text-sm">{check.reason}</p>)}
          </section>
        )}
        <VerificationImage key={`${challengeId}-${scheduleId}`} onImageChange={handleImageChange} />

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
        <SuccessButton onClick={handleVerification} disabled={submitting || !image || !!refreshError || (remainingAttempts === 0 && !submitError)
          || result?.status === 'SUCCESS' || result?.status === 'REVIEW_REQUIRED' || result?.status === 'FAILED'}>
          인증하기
        </SuccessButton>
      </div>
      </div>
    </div>
  )
}
