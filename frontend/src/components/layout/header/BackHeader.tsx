interface BackHeaderProps {
  title: string
}

export function BackHeader({ title }: BackHeaderProps) {
    return (
    <header className="header">
        <svg xmlns="http://www.w3.org/2000/svg" width="1em" height="1em" viewBox="0 0 24 24">
            <path d="M0 0h24v24H0z" fill="none" />
            <path fill="none" stroke="currentColor" stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="m15 6l-6 6l6 6" />
        </svg>
        <h1 className="text-description">{title}</h1>
    </header>
    )
}