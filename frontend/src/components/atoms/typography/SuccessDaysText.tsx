interface SuccessDaysTextProps {
  durationDays: number
  successDays: number
  className?: string
}

export function SuccessDaysText({ durationDays, successDays, className = '' }: SuccessDaysTextProps) {
  return (
    <p className={`m-0 font-[family-name:var(--font-ui)] text-[12px] leading-4 font-normal text-[#4A382A] ${className}`}>
      {durationDays}일 중 {successDays}일 성공
    </p>
  )
}
