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

const checkColors = {
  MET: 'bg-[#e1e9d5] text-[#426b45]',
  NOT_MET: 'bg-[#f2c5b3] text-[#934f3d]',
  UNCERTAIN: 'bg-[#f7e6be] text-[#806126]',
}

// 조건 코드를 기준으로 서버가 제공한 설명과 판정 사유 표시
export function VerificationChecks({ checks }: VerificationChecksProps) {
  return (
    <section className="space-y-2 rounded-[10px] border border-[var(--color-border)] bg-white px-3 py-3.5" aria-label="인증 조건별 결과">
      {checks.map(check => (
        <div key={check.code} className="flex items-start gap-2 text-[13px] leading-[1.6]">
          <span className={`shrink-0 rounded-xl px-2 py-px text-[11px] font-semibold ${checkColors[check.status]}`}>
            {checkLabels[check.status]}
          </span>
          <div>
            <p>{check.description || check.reason || check.code}</p>
            {check.description && check.reason && check.description !== check.reason && (
              <p className="mt-0.5 text-[12px] text-[var(--color-text-muted)]">{check.reason}</p>
            )}
          </div>
        </div>
      ))}
      {checks.length === 0 && <p>저장된 조건별 결과가 없어요.</p>}
    </section>
  )
}

