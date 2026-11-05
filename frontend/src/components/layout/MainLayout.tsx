import { Outlet } from 'react-router'
import { MainHeader } from './header/MainHeader'
import { BackHeader } from './header/BackHeader'
import { TitleHeader } from './header/TitleHeader'
import { LoginHeader } from './header/LoginHeader'
import { BottomNavigation } from './navigation/BottomNavigation'
import '../../styles/font.css'

interface MainLayoutProps {
  headerType?: 'main' | 'back' | 'title' | 'login'
  title?: string
  showBottomNav?: boolean
  background?: 'white' | 'transparent'
}

export function MainLayout({
  headerType = 'main',
  title = '',
  showBottomNav = false,
  background = 'transparent',
}: MainLayoutProps) {
  const renderHeader = () => {
    switch (headerType) {
      case 'back':
        return <BackHeader title={title} />
      case 'title':
        return <TitleHeader title={title} />
      case 'login':
        return <LoginHeader />
      default:
        return <MainHeader />
    }
  }

  return (
    <div className="flex min-h-dvh w-full flex-col">
      {renderHeader()}

      <main
        className={`
          m-0 flex-1
          ${showBottomNav ? 'pb-28' : ''}
          ${background === 'white' ? 'bg-white' : ''}
        `}
      >
        <Outlet />
      </main>

      {showBottomNav && <BottomNavigation />}
    </div>
  )
}