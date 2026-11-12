
import { useEffect, useRef, useState } from 'react'
import { Icon } from '../../atoms/icon/Icon'

interface VerificationImageProps {
  onImageChange: (file: File | null) => void
}

export function VerificationImage({
  onImageChange,
}: VerificationImageProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [preview, setPreview] = useState<string | null>(null)

  // 미리보기 URL 정리
  useEffect(() => {
    return () => {
      if (preview) {
        URL.revokeObjectURL(preview)
      }
    }
  }, [preview])

  // 이미지 선택
  const handleImageChange = (
    e: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file = e.target.files?.[0]

    if (!file) return

    if (!file.type.startsWith('image/')) {
      e.target.value = ''
      return
    }

    const imageUrl = URL.createObjectURL(file)

    setPreview(imageUrl)
    onImageChange(file)
  }

  return (
    <div
      className={`relative w-full aspect-[1.56] overflow-hidden rounded-md bg-[#FFF8ED] ${
        !preview ? 'border border-dashed border-[#D8C3A5]' : ''
      }`}
    >
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={handleImageChange}
      />

      {preview ? (
        <>
          <img
            src={preview}
            alt="인증 사진 미리보기"
            className="h-full w-full object-cover"
          />

          <button
            type="button"
            onClick={() => inputRef.current?.click()}
            className="absolute bottom-3 right-3 rounded-full border border-[#6B5B4B] bg-[#FFF8ED] px-3 py-1 text-sm text-[#4B3828]"
          >
            다시 고르기
          </button>
        </>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          className="flex h-full w-full flex-col items-center justify-center gap-2 text-[#4B3828]"
        >
          <Icon name="camera" size={32} />
          <span className="text-sm">
            사진을 등록해주세요
          </span>
        </button>
      )}
    </div>
  )
}