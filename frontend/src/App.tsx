function App() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-amber-50 px-6">
      <div className="w-full max-w-md rounded-3xl bg-white p-8 text-center shadow-lg">
        <h1 className="mb-4 text-4xl font-bold text-emerald-600">
          모아뮤
        </h1>

        <p className="mb-6 text-gray-600">
          작은 실천으로 완성하는 나만의 작품
        </p>

        <button
          type="button"
          className="rounded-xl bg-emerald-500 px-6 py-3 font-semibold text-white transition-colors hover:bg-emerald-600"
        >
          시작하기
        </button>
      </div>
    </main>
  )
}

export default App
