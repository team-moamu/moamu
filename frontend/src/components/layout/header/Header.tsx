import { LoginHeader } from './LoginHeader'
import { BackHeader } from './BackHeader'
import { TitleHeader } from './TitleHeader'
import { MainHeader } from './MainHeader'

type HeaderProps =
  | { variant: 'login' }
  | { variant: 'back'; title: string }
  | { variant: 'title'; title: string }
  | { variant: 'main' }

export function Header(props: HeaderProps) {
  switch (props.variant) {
    case 'login':
      return <LoginHeader />

    case 'back':
      return <BackHeader title={props.title} />

    case 'title':
      return <TitleHeader title={props.title} />

    case 'main':
      return <MainHeader />
  }
}