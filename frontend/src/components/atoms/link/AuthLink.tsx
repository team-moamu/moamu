import { Link } from 'react-router'

type AuthLinkProps = {
  to: string
  children: React.ReactNode
}

export function AuthLink({ to, children }: AuthLinkProps) {
  return (
    <Link to={to} className="text-caption text-[#426B45]">
      {children}
    </Link>
  )
}
