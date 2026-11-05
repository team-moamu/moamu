type TextProps = {
  children: React.ReactNode
  className?: string
}

export function Text({ children, className = '' }: TextProps) {
  return (
    <span className={`text-description text-[#4A382A] ${className}`}>
      {children}
    </span>
  )
}