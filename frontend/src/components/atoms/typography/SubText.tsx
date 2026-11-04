type SubTextProps = {
  children: React.ReactNode
  bold?: boolean
}

export function SubText({ children, bold = false }: SubTextProps) {
  return (
    <span
      className={`text-xs text-[#6B5B4B] ${
        bold ? 'font-bold' : 'font-normal'
      }`}
    >
      {children}
    </span>
  )
}