type FormLabelProps = {
  htmlFor: string
  children: React.ReactNode
  className?: string
}

export function FormLabel({ htmlFor, children, className = '' }: FormLabelProps) {
  return <label htmlFor={htmlFor} className={`text-label ${className}`}>{children}</label>
}
