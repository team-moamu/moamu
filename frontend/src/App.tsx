import { useState } from 'react'

const menus = [
  { id: 'home', label: '홈', icon: 'icon-[lucide--house]' },
  { id: 'challenge', label: '도전하기', icon: 'icon-[lucide--puzzle]' },
  { id: 'museum', label: '미술관', icon: 'icon-[lucide--landmark]' },
  { id: 'collection', label: '보관함', icon: 'icon-[lucide--archive]' },
  { id: 'mypage', label: '마이', icon: 'icon-[lucide--user-round]' },
] as const

function App() {
  const [activeMenu, setActiveMenu] = useState<string>('home')

  return (
    <div className="flex min-h-screen items-center justify-center bg-stone-100 p-4">
      <div className="flex h-[760px] w-full max-w-[393px] flex-col overflow-hidden rounded-[32px] border-2 border-[#604d3b] bg-[#fffaf1] shadow-xl">

        <header className="border-b border-stone-200 px-6 py-5">
          <h1 className="text-2xl font-bold text-[#604d3b]">
            MOAMU
          </h1>
        </header>

        <main className="flex flex-1 flex-col items-center justify-center gap-4 px-4 text-center">
          <h2 className="text-2xl font-bold text-[#426c46]">
            Iconify 테스트
          </h2>
          <p className="text-sm text-[#604d3b]">
            하단 아이콘을 눌러보세요!
          </p>
          <p className="rounded-xl bg-[#e0e9d7] px-6 py-3 font-semibold text-[#426c46]">
            선택 메뉴: {menus.find((menu) => menu.id === activeMenu)?.label}
          </p>
        </main>

        <nav
          aria-label="하단 메뉴"
          className="grid grid-cols-5 border-t border-[#e5d9c8] bg-white px-2 py-3"
        >
          {menus.map((menu) => (
            <button
              key={menu.id}
              type="button"
              onClick={() => setActiveMenu(menu.id)}
              aria-current={activeMenu === menu.id ? 'page' : undefined}
              className={`flex flex-col items-center justify-center gap-1 ${
                activeMenu === menu.id
                  ? 'text-[#426c46]'
                  : 'text-[#604d3b]'
              }`}
            >
              <span
                aria-hidden="true"
                className={`${menu.icon} text-[27px]`}
              />
              <span className="text-[10px]">{menu.label}</span>
            </button>
          ))}
        </nav>

        <div className="flex justify-center bg-white pb-2 pt-1">
          <div className="h-1 w-28 rounded-full bg-[#604d3b]" />
        </div>

      </div>
    </div>
  )
}

export default App
