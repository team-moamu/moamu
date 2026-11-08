import { useEffect, useState } from 'react'
import { useLocation, useNavigate, useParams } from 'react-router'

import sadMu from '../../assets/images/sad_mu.png'
import { getVerificationSuccess } from '../../api/VerificationApi'
import { getChallengeVerification } from '../../api/ChallengeApi'
import { VerificationChecks } from '../../components/molecules/feedback/VerificationChecks'
import { VerificationPuzzle } from '../../components/organisms/puzzle/VerificationPuzzle'
import { BottomNavigation } from '../../components/layout/navigation/BottomNavigation'
import type { ChallengeVerificationResponse, VerificationSubmitResponse, VerificationSuccessResponse } from '../../types/Challenge'
import './VerificationResultPage.css'

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

  useEffect(() => {
    const controller = new AbortController()
    async function fetchResult() {
      setLoading(true)
      setError(null)
      setSuccessResult(null)
      setRemainingAttempts(null)
      try {
        const id = Number(verificationId)
        if (!Number.isSafeInteger(id) || id <= 0) throw new Error('올바르지 않은 인증 결과입니다.')

        if (isSuccess) {
          const result = await getVerificationSuccess(id, controller.signal, accessToken)
          if (result.challengeId !== Number(challengeId)) throw new Error('도전 정보가 일치하지 않습니다.')
          if (!controller.signal.aborted) setSuccessResult(result)
        } else if (submission) {
          // 실패 후 재인증 가능 여부는 최신 회차의 시도 횟수로 확인
          const challenge = await getChallengeVerification(
            submission.challenge.challengeId, submission.challenge.scheduleId, controller.signal, accessToken,
          )
          if (!challenge) throw new Error('회차 정보를 찾을 수 없어요.')
          if (!controller.signal.aborted) setRemainingAttempts(Math.max(0, MAX_ATTEMPTS - challenge.attemptCount))
        }
      } catch (err) {
        if (!controller.signal.aborted) setError(err instanceof Error ? err.message : '결과를 불러오지 못했어요.')
      } finally {
        if (!controller.signal.aborted) setLoading(false)
      }
    }
    fetchResult()
    return () => controller.abort()
  }, [challengeId, verificationId, isSuccess, submission, accessToken, reloadCount])

  const retryVerification = () => {
    if (!submission || remainingAttempts === null || remainingAttempts <= 0 || isReview) return
    navigate(`/challenge/${challengeId}/verification?scheduleId=${submission.challenge.scheduleId}`, { replace: true })
  }

  return (
    <div className={`verification-result-page ${isSuccess ? '' : 'has-bottom-nav'}`}>
      <main className="verification-result-content" aria-busy={loading}>
        {isSuccess && (
          <button className="verification-result-close" type="button" aria-label="닫고 홈으로 이동" onClick={() => navigate('/')}>×</button>
        )}
        <header className="verification-result-header">
          <h1>{isSuccess ? '오늘도 성공했어요!' : isReview ? '확인이 필요한 인증이에요' : '인증에 실패했어요'}</h1>
          <p>{isSuccess ? '새로운 조각을 발견했어요' : isReview
            ? '판단이 어려운 조건이 있어 검토가 필요해요.' : '아래 내용을 확인하고 다시 시도해 주세요.'}</p>
        </header>

        {isSuccess ? (
          <>
            {successResult && (
              <>
                <VerificationPuzzle result={successResult} />
                <VerificationChecks checks={successResult.checks} />
                <p className="verification-result-encouragement">꾸준히 하는 모습이 정말 멋져요!</p>
              </>
            )}
          </>
        ) : (
          <>
            <img className="verification-result-character" src={sadMu} alt={isReview ? '검토 결과를 기다리는 뮤' : '인증에 실패해 속상한 뮤'} />
            {submission && <VerificationChecks checks={submission.result.checks} />}
            <section className="verification-attempts" aria-label="남은 인증 시도">
              <strong>남은 시도</strong>
              <span>{remainingAttempts === null ? '확인 중' : `${remainingAttempts} / ${MAX_ATTEMPTS}`}</span>
            </section>
            {!isReview && remainingAttempts === 0 && <p className="verification-result-encouragement">오늘의 인증 기회를 모두 사용했어요.</p>}
          </>
        )}

        {loading && <p className="verification-result-notice" role="status">결과를 불러오는 중이에요.</p>}
        {error && (
          <div className="verification-result-notice" role="alert">
            <p>{error}</p>
            <button type="button" onClick={() => setReloadCount(count => count + 1)}>다시 불러오기</button>
          </div>
        )}

        <div className="verification-result-actions">
          {isSuccess ? (
            <>
              <button className="verification-result-primary" type="button" onClick={() => navigate('/challenge/complete')}>내 미술관 보러가기</button>
              <button className="verification-result-secondary" type="button" onClick={() => navigate('/')}>홈으로 돌아가기</button>
            </>
          ) : (
            <>
              <button className="verification-result-primary" type="button"
                disabled={loading || !!error || isReview || remainingAttempts === null || remainingAttempts <= 0}
                onClick={retryVerification}>{isReview ? '검토 대기 중' : '다시 인증하기'}</button>
              <button className="verification-result-secondary" type="button" onClick={() => navigate('/challenge/list')}>
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

