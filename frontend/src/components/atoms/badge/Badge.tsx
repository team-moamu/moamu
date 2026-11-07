export type BadgeVariant =
  | 'available'
  | 'retry'
  | 'success'
  | 'fail'
  | 'processing'
  | 'review'
  | 'scheduled'
  | 'settlement'

type BadgeProps = {
  children: string
  variant: BadgeVariant
}

export function Badge({ children, variant }: BadgeProps) {
  const variantStyle: Record<BadgeVariant, string> = {
    // 인증 가능
    available: 'bg-green-100 text-green-700',
    // 재시도
    retry: 'bg-orange-100 text-orange-700',
    // 성공
    success: 'bg-[#DFEAD8] text-[#4F7652]',
    // 실패
    fail: 'bg-[#EDB6A2] text-[#A65F4B]',
    // AI 인증 처리 중
    processing: 'bg-[#E2EDF3] text-[#5A8299]',
    // 운영 검토 중
    review: 'bg-blue-100 text-blue-700',
    // 예정
    scheduled: 'bg-[#F4E8D6] text-[#6B5B4B]',
    // 정산 대기
    settlement: 'bg-yellow-100 text-yellow-700',
  }

  return (
    <span
      className={`inline-flex w-20 items-center justify-center
        rounded-full px-3 py-1 text-xs font-medium
        ${variantStyle[variant]}`}
    >
      {children}
    </span>
  )
}