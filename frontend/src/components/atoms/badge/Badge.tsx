type BadgeProps = {
  children: string
  variant: 'available' | 'retry'
}

export function Badge({ children, variant }: BadgeProps) {
  const variantStyle = {
    // 인증 가능
    available: 'bg-green-100 text-green-700',
    // 인증 불가
    retry: 'bg-orange-100 text-orange-700',
  }

  return (
    <span
      className={`w-20 text-center inline-block rounded-full px-3 py-1 text-xs
        ${variantStyle[variant]}`}
    >
      {children}
    </span>
  )
}