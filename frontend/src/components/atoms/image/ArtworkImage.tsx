import { useState } from 'react'

interface ArtworkImageProps {
  imageUrl: string | null
  artworkTitle: string
  className?: string
}

export function ArtworkImage({ imageUrl, artworkTitle, className = '' }: ArtworkImageProps) {
  const [failedUrl, setFailedUrl] = useState<string | null>(null)
  const showImage = Boolean(imageUrl && imageUrl !== failedUrl)

  return (
    <div className={`h-[60px] w-[60px] shrink-0 overflow-hidden rounded-[4px] bg-[#F2E6D2] ${className}`}>
      {showImage ? (
        <img
          src={imageUrl!}
          alt={artworkTitle}
          loading="lazy"
          className="h-full w-full object-cover"
          onError={() => setFailedUrl(imageUrl)}
        />
      ) : (
        <span role="img" aria-label={`${artworkTitle}: 이미지 없음`} className="flex h-full items-center justify-center text-[10px] text-[#8A7762]">
          이미지 없음
        </span>
      )}
    </div>
  )
}
