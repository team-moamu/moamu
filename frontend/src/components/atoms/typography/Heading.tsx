type HeadingProps = {
  children: React.ReactNode
  className?: string
}

export function Heading({ children, className = '' }: HeadingProps) {
  return (
    <h1 className={`text-description text-description--header ${className}`}>
      {children}
    </h1>
  )
}
