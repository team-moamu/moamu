import { useEffect, useRef } from 'react'
import { createPortal } from 'react-dom'
import loadingBackground from '../../../assets/images/verification_loading_mu.png'
import './VerificationLoading.css'

type VerificationLoadingProps = { challengeTitle: string } & (
  | { mode?: never; dayNo: number; dayCount: number }
  | { mode?: 'artwork'; dayNo?: never; dayCount?: never }
)

// AI 응답을 기다리는 동안 기존 화면의 입력 상태를 유지한다.
export function VerificationLoading(props: VerificationLoadingProps) {
  const screenRef = useRef<HTMLElement>(null)

  useEffect(() => {
    const root = document.getElementById('root')
    const previousInert = root?.inert ?? false
    const previousOverflow = document.body.style.overflow
    const previousFocus = document.activeElement

    // 로딩 화면 뒤의 헤더와 하단 메뉴도 키보드로 조작되지 않도록 처리
    if (root) root.inert = true
    document.body.style.overflow = 'hidden'
    screenRef.current?.focus()

    return () => {
      if (root) root.inert = previousInert
      document.body.style.overflow = previousOverflow
      if (previousFocus instanceof HTMLElement && previousFocus.isConnected) previousFocus.focus()
    }
  }, [])

  return createPortal(
    <section
      ref={screenRef}
      className="verification-loading"
      role="dialog"
      aria-modal="true"
      aria-labelledby="verification-loading-title"
      aria-describedby="verification-loading-description"
      aria-busy="true"
      tabIndex={-1}
    >
      <VerificationLoadingContent {...props} />
    </section>,
    document.body,
  )
}

export function VerificationLoadingContent({ challengeTitle, mode, dayNo, dayCount }: VerificationLoadingProps) {
  const [title, message] = mode === 'artwork'
    ? ['도전 작품을 만드는 중…', '나만의 목표에 어울리는 작품을 만들고 있어요']
    : dayNo === undefined
      ? ['목표를 분석하는 중…', '목표의 사진 인증 조건을 살펴보고 있어요']
      : ['사진을 분석하는 중…', '사진에서 오늘의 습관을 찾아보고 있어요']

  return (
    <>
      <img className="verification-loading__background" src={loadingBackground} alt="" />
      <div className="verification-loading__wash" aria-hidden="true" />

      <div className="verification-loading__content">
        <h1 id="verification-loading-title" className="verification-loading__title">
          {title}
        </h1>

        <section className="verification-loading__card" aria-label="나의 도전">
          <h2>나의 도전</h2>
          <p className="verification-loading__challenge">{challengeTitle}</p>
          {dayNo !== undefined && <p>Day {dayNo} · {dayCount}일 도전</p>}
        </section>

        <div className="verification-loading__message" role="status">
          <p>{message}</p>
          <p id="verification-loading-description">잠시만 기다려 주세요</p>
        </div>

        <div className="verification-loading__spinner" aria-hidden="true">
          {Array.from({ length: 12 }, (_, index) => (
            <span key={index} style={{ transform: `rotate(${index * 30}deg) translateY(-44px)`, opacity: (12 - index) / 12 }} />
          ))}
        </div>
      </div>
    </>
  )
}
