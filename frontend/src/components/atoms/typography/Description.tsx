type DescriptionProps = {
  children: React.ReactNode
  className?: string
}

export function Description({ children, className = '' }: DescriptionProps) {
  return <p className={`text-caption ${className}`}>{children}</p>
}
