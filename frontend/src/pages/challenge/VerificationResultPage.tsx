import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router'

import sadMu from '../../assets/images/sad_mu.png'
import { getVerificationSuccess } from '../../api/VerificationApi'
import { ChallengeCompletionError, getChallengeCompletion, getChallengeVerification } from '../../api/ChallengeApi'
import { VerificationChecks } from '../../components/molecules/feedback/VerificationChecks'
import { VerificationPuzzle } from '../../components/organisms/puzzle/VerificationPuzzle'
import { BottomNavigation } from '../../components/layout/navigation/BottomNavigation'
import type { ChallengeVerificationResponse, VerificationSubmitResponse, VerificationSuccessResponse } from '../../types/Challenge'

export interface VerificationResultLocationState {
  result: VerificationSubmitResponse
  challenge: ChallengeVerificationResponse
}

interface VerificationResultPageProps {
  accessToken?: string
}

const MAX_ATTEMPTS = 3

export function VerificationResultPage({ accessToken }: VerificationResultPageProps) {
  const { challengeId, verificationId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const state = location.state as VerificationResultLocationState | null
  // 이동 상태는 현재 URL의 인증 결과와 일치할 때만 사용
  const submission = state?.result.verificationId === Number(verificationId)
    && state.challenge.challengeId === Number(challengeId) ? state : null
  const status = submission?.result.status ?? 'SUCCESS'
  const isSuccess = status === 'SUCCESS'
  const isReview = status === 'REVIEW_REQUIRED'
  const [successResult, setSuccessResult] = useState<VerificationSuccessResponse | null>(null)
  const [remainingAttempts, setRemainingAttempts] = useState<number | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadCount, setReloadCount] = useState(0)
  const [completionPending, setCompletionPending] = useState(false)

  useEffect(() => {
    const controller = new AbortController()
    async function openCompletionIfReady() {
      try {
        await getChallengeCompletion(Number(challengeId), controller.signal, accessToken)
        if (!controller.signal.aborted) {
          navigate(`/challenge/${challengeId}/completion`, { replace: true })
        }
      } catch (err) {
        if (err instanceof ChallengeCompletionError && err.code === 'CHALLENGE_NOT_COMPLETED') {
          if (!controller.signal.aborted) setCompletionPending(true)
        } else {
          throw err
        }
      }
    }
    async function fetchResult() {
      setLoading(true)
      setError(null)
      setSuccessResult(null)
      setRemainingAttempts(null)
      setCompletionPending(false)
      try {
        const id = Number(verificationId)
        if (!Number.isSafeInteger(id) || id <= 0) throw new Error('올바르지 않은 인증 결과입니다.')

        if (isSuccess) {
          const result = await getVerificationSuccess(id, controller.signal, accessToken)
          if (result.challengeId !== Number(challengeId)) throw new Error('도전 정보가 일치하지 않습니다.')
          if (!controller.signal.aborted) setSuccessResult(result)
          if (result.dayNo === result.totalPieceCount) {
            await openCompletionIfReady()
          }
        } else if (submission) {
          // 실패 후 재인증 가능 여부는 최신 회차의 시도 횟수로 확인
          const challenge = await getChallengeVerification(
            submission.challenge.challengeId, submission.challenge.scheduleId, controller.signal, accessToken,
          )
          if (!challenge) throw new Error('회차 정보를 찾을 수 없어요.')
          if (!controller.signal.aborted) setRemainingAttempts(Math.max(0, MAX_ATTEMPTS - challenge.attemptCount))
          if (!isReview && challenge.dayNo === challenge.dayCount && challenge.attemptCount >= MAX_ATTEMPTS) {
            await openCompletionIfReady()
          }
        }
      } catch (err) {
        if (!controller.signal.aborted) setError(err instanceof Error ? err.message : '결과를 불러오지 못했어요.')
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }
    fetchResult()
    return () => controller.abort()
  }, [challengeId, verificationId, isSuccess, isReview, submission, accessToken, reloadCount, navigate])

  const retryVerification = () => {
    if (!submission || remainingAttempts === null || remainingAttempts <= 0 || isReview) return
    navigate(`/challenge/${challengeId}/verification?scheduleId=${submission.challenge.scheduleId}`, { replace: true })
  }

  if (loading) {
    return <main className="flex min-h-dvh items-center justify-center bg-[var(--color-background)] px-6 text-center" role="status">인증 결과를 확인하고 있어요.</main>
  }

  return (
    <div className={`min-h-dvh bg-[var(--color-background)] text-[var(--color-text)] [&_button:focus-visible]:outline-[3px] [&_button:focus-visible]:outline-[#b88a3c] [&_button:focus-visible]:outline-offset-[3px] ${isSuccess ? '' : 'pb-[calc(88px+env(safe-area-inset-bottom))]'}`}>
      <main className={`relative mx-auto flex w-full max-w-[430px] flex-col px-6 pt-16 max-[350px]:px-4 ${isSuccess ? 'min-h-dvh pb-[max(28px,env(safe-area-inset-bottom))]' : 'min-h-[calc(100dvh-88px)] pb-7'}`} aria-busy={loading}>
        {isSuccess && (
          <button className="absolute top-7 right-4 h-11 w-11 cursor-pointer text-[32px] leading-none text-[var(--color-text-muted)]" type="button" aria-label="닫고 홈으로 이동" onClick={() => navigate('/')}>×</button>
        )}
        <header className="mb-5 text-center">
          <h1 className="[font-family:var(--font-description)] text-[26px] font-bold leading-[1.4]">{isSuccess ? '오늘도 성공했어요!' : isReview ? '확인이 필요한 인증이에요' : '인증에 실패했어요'}</h1>
          <p className="mt-2 text-[13px]">{isSuccess ? '새로운 조각을 발견했어요' : isReview
            ? '판단이 어려운 조건이 있어 검토가 필요해요.' : '아래 내용을 확인하고 다시 시도해 주세요.'}</p>
        </header>

        {isSuccess ? (
          <>
            {successResult && (
              <>
                <VerificationPuzzle result={successResult} />
                <VerificationChecks checks={successResult.checks} />
                <p className="mt-2 text-center text-[12px]">꾸준히 하는 모습이 정말 멋져요!</p>
              </>
            )}
          </>
        ) : (
          <>
            <img className="mt-2 mb-6 h-[280px] w-full max-w-[310px] self-center object-contain" src={sadMu} alt={isReview ? '검토 결과를 기다리는 뮤' : '인증에 실패해 속상한 뮤'} />
            {submission && <VerificationChecks checks={submission.result.checks} />}
            <section className="mt-3.5 flex items-center justify-between rounded-[10px] border border-[var(--color-border)] p-3 text-[13px] shadow-[0_1px_4px_rgb(73_55_37/12%)]" aria-label="남은 인증 시도">
              <strong className="[font-family:var(--font-description)] text-[18px]">남은 시도</strong>
              <span>{remainingAttempts === null ? '확인 중' : `${remainingAttempts} / ${MAX_ATTEMPTS}`}</span>
            </section>
            {!isReview && remainingAttempts === 0 && <p className="mt-2 text-center text-[12px]">오늘의 인증 기회를 모두 사용했어요.</p>}
          </>
        )}

        {completionPending && (
          <div className="mt-4 text-center text-[13px]" role="status">
            <p>아직 확정되지 않은 인증이 있어요. 모든 결과가 확정되면 도전 완료를 확인할 수 있어요.</p>
            <button className="cursor-pointer px-4 py-2.5 underline" type="button" onClick={() => setReloadCount(count => count + 1)}>완료 상태 다시 확인</button>
          </div>
        )}
        {error && (
          <div className="mt-4 text-center text-[13px]" role="alert">
            <p>{error}</p>
            <button className="cursor-pointer px-4 py-2.5 underline" type="button" onClick={() => setReloadCount(count => count + 1)}>다시 불러오기</button>
          </div>
        )}

        <div className="mt-auto grid gap-2.5 pt-10">
          {isSuccess ? (
            <>
              <button className="min-h-12 w-full cursor-pointer rounded-[11px] bg-[var(--color-primary)] p-3 text-[14px] text-white disabled:cursor-not-allowed disabled:bg-[#d9d9d9] disabled:text-[#777]" type="button" onClick={() => navigate('/museum')}>내 미술관 보러가기</button>
              <button className="min-h-12 w-full cursor-pointer rounded-[11px] border border-[var(--color-border)] bg-white p-3 text-[14px]" type="button" onClick={() => navigate('/')}>홈으로 돌아가기</button>
            </>
          ) : (
            <>
              <button className="min-h-12 w-full cursor-pointer rounded-[11px] bg-[var(--color-primary)] p-3 text-[14px] text-white disabled:cursor-not-allowed disabled:bg-[#d9d9d9] disabled:text-[#777]" type="button"
                disabled={loading || !!error || isReview || remainingAttempts === null || remainingAttempts <= 0}
                onClick={retryVerification}>{isReview ? '검토 대기 중' : '다시 인증하기'}</button>
              <button className="min-h-12 w-full cursor-pointer rounded-[11px] border border-[var(--color-border)] bg-white p-3 text-[14px]" type="button" onClick={() => navigate('/challenge/list')}>
                {isReview ? '도전 목록으로 돌아가기' : '오늘 인증 넘기기'}
              </button>
            </>
          )}
        </div>
      </main>
      {!isSuccess && <BottomNavigation />}
    </div>
  )
}

