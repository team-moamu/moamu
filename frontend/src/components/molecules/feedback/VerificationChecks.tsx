import type { VerificationSubmitResponse } from '../../../types/Challenge'

type VerificationCheck = VerificationSubmitResponse['checks'][number] & { description?: string }

interface VerificationChecksProps {
  checks: VerificationCheck[]
}

const checkLabels = {
  MET: '충족',
  NOT_MET: '미충족',
  UNCERTAIN: '판단 보류',
}

// 조건 코드를 기준으로 서버가 제공한 설명과 판정 사유 표시
export function VerificationChecks({ checks }: VerificationChecksProps) {
  return (
    <section className="verification-checks" aria-label="인증 조건별 결과">
      {checks.map(check => (
        <div key={check.code} className="verification-check">
          <span className={`verification-check-badge verification-check-badge--${check.status.toLowerCase()}`}>
            {checkLabels[check.status]}
          </span>
          <div>
            <p>{check.description || check.reason || check.code}</p>
            {check.description && check.reason && check.description !== check.reason && (
              <p className="verification-check-reason">{check.reason}</p>
            )}
          </div>
        </div>
      ))}
      {checks.length === 0 && <p>저장된 조건별 결과가 없어요.</p>}
    </section>
  )
}

