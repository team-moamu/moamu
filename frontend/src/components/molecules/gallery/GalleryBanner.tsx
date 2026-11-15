import backgroundMu from '../../../assets/images/background_mu.png'
import leaf from '../../../assets/icons/leaf.svg'

export function GalleryBanner() {
  return (
    <section aria-label="내 미술관" className="relative">
      <img src={backgroundMu} alt="" className="block w-full" />

      <div className="absolute top-[calc(25%+2px)] left-1/2 w-[76%] -translate-x-1/2 text-center text-[#4A382A]">
        <h2 className="m-0 font-[family-name:var(--font-description)] text-[18px] leading-[1.4] font-bold break-keep">
          나의 작은 미술관
        </h2>

        <p className="mt-[6px] mb-0 font-[family-name:var(--font-ui)] text-[12px] leading-[1.6] font-normal">
          하루하루의 기록이
          <br />
          <span className="relative inline-block font-semibold">
            <img
              src={leaf}
              alt=""
              className="pointer-events-none absolute bottom-[calc(100%-8px)] left-full h-4 w-4 -translate-x-1/2"
            />
            작품
          </span>으로 남아요.
        </p>
      </div>
    </section>
  )
}
