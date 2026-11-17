import defaultProfileMu from '../../assets/images/default_profile_mu.png'
import { previewNickname } from './profilePreview'
import { useEffect, useState } from 'react'

export function ProfileEditPage() {
  const [photo, setPhoto] = useState<File | null>(null)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [isPhotoMenuOpen, setIsPhotoMenuOpen] = useState(false)

  useEffect(() => {
    if (!photo) {
      setPreviewUrl(null)
      return
    }

    const url = URL.createObjectURL(photo)
    setPreviewUrl(url)

    return () => URL.revokeObjectURL(url)
  }, [photo])

  return (
    <div className="space-y-6 px-6 py-6">
      <div className="relative mx-auto w-fit">
        <img
          src={previewUrl ?? defaultProfileMu}
          alt="프로필 사진 미리보기"
          className="h-24 w-24 rounded-full object-cover"
        />
        <button
          type="button"
          aria-label="프로필 사진 변경"
          onClick={() => setIsPhotoMenuOpen(true)}
          className="absolute right-0 bottom-0 grid h-8 w-8 place-items-center rounded-full border border-[#E8CFAC] bg-white text-accent"
        >
          <span aria-hidden="true">✎</span>
        </button>
      </div>

      <div>
        <label htmlFor="profile-nickname" className="mb-2 block font-bold">
          닉네임
        </label>
        <input
          id="profile-nickname"
          name="nickname"
          type="text"
          defaultValue={previewNickname}
          aria-describedby="profile-nickname-help"
          className="w-full rounded-xl border border-neutral-200 bg-white px-4 py-3"
        />
        <p id="profile-nickname-help" className="mt-2 text-sm text-neutral-600">
          다른 사람에게 보이는 이름이에요.
        </p>
      </div>

      <section className="rounded-2xl bg-white p-4">
        <h2 className="font-bold">프로필 사진 안내</h2>
        <p className="mt-2 text-sm text-neutral-600">
          사진을 선택하지 않으면 기본 이미지가 보여요.
        </p>
      </section>

      {isPhotoMenuOpen && (
        <div className="fixed inset-y-0 left-1/2 z-50 flex w-full max-w-[430px] -translate-x-1/2 items-end justify-center bg-black/40">          <section
          aria-labelledby="photo-menu-heading"
          className="w-full max-w-md rounded-t-2xl bg-white p-6"
        >
          <h2 id="photo-menu-heading" className="mb-4 text-lg font-bold">
            프로필 사진
          </h2>

          <div className="grid gap-2">
            <label className="relative block cursor-pointer rounded-xl border border-[#E8CFAC] px-4 py-3 text-center focus-within:ring-2 focus-within:ring-accent">
              사진 선택하기
              <input
                type="file"
                accept="image/*"
                className="sr-only"
                onChange={(event) => {
                  const file = event.currentTarget.files?.[0]
                  if (!file) return

                  setPhoto(file)
                  setIsPhotoMenuOpen(false)
                }}
              />
            </label>

            <button
              type="button"
              className="rounded-xl border border-[#E8CFAC] px-4 py-3"
              onClick={() => {
                setPhoto(null)
                setIsPhotoMenuOpen(false)
              }}
            >
              기본 이미지로 변경
            </button>

            <button
              type="button"
              className="rounded-xl px-4 py-3 text-neutral-600"
              onClick={() => setIsPhotoMenuOpen(false)}
            >
              취소
            </button>
          </div>
        </section>
        </div>
      )}
    </div>
  )
}