import defaultProfileMu from '../../assets/images/default_profile_mu.png'
import { useNavigate } from 'react-router'
import { useEffect, useState } from 'react'

type ProfileEditPageProps = {
  nickname: string
  profilePhoto: File | null
  onSave: (nickname: string, photo: File | null) => Promise<void>
}

export function ProfileEditPage({
  nickname,
  profilePhoto,
  onSave,
}: ProfileEditPageProps) {
  const navigate = useNavigate()
  const [draftNickname, setDraftNickname] = useState(nickname)
  const trimmedNickname = draftNickname.trim()
  const [photo, setPhoto] = useState<File | null>(profilePhoto)
  const [previewUrl, setPreviewUrl] = useState<string | null>(null)
  const [isPhotoMenuOpen, setIsPhotoMenuOpen] = useState(false)
  const [isSaving, setIsSaving] = useState(false)
  const [saveError, setSaveError] = useState('')

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
    <form
      className="space-y-6 px-6 pt-6 pb-28"
      onSubmit={async (event) => {
        event.preventDefault()

        if (!trimmedNickname || isSaving) return

        setIsSaving(true)
        setSaveError('')

        try {
          await onSave(trimmedNickname, photo)

          navigate('/mypage', {
            replace: true,
            state: { profileSaved: true },
          })
        } catch (error) {
          setSaveError(
            error instanceof Error ? error.message : '닉네임을 저장하지 못했어요.',
          )
        } finally {
          setIsSaving(false)
        }
      }}
    >
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
          value={draftNickname}
          onChange={(event) => setDraftNickname(event.currentTarget.value)}
          aria-describedby="profile-nickname-help"
          className="w-full rounded-xl border border-neutral-200 bg-white px-4 py-3"
        />
        <p id="profile-nickname-help" className="mt-2 text-sm text-neutral-600">
          다른 사람에게 보이는 이름이에요.
        </p>
      </div>

      <section className="flex min-h-[80px] flex-col justify-center gap-[8px] rounded-[12px] bg-[rgba(66,107,69,0.2)] px-3 py-3">
        <h2 className="text-[16px] leading-[24px] font-bold text-[var(--color-primary)]">
          프로필 사진 안내
        </h2>
        <p className="text-[12px] leading-[18px] font-normal text-black/50">
          사진을 선택하지 않으면 기본 이미지가 보여요.
        </p>
      </section>
      <div className="fixed inset-x-0 bottom-0 z-10 mx-auto w-full max-w-[430px] px-6 pb-[max(1.5rem,env(safe-area-inset-bottom))]">
        <button
          type="submit"
          disabled={!trimmedNickname || isSaving}
          className="button button--primary button--full h-[52px]"
        >
          {isSaving ? '저장 중…' : '변경사항 저장'}
        </button>
      </div>

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
    </form>
  )
}