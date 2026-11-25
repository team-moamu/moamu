type BlurImageProps = {
  src: string
  alt: string
  className?: string
  alreadyBlurred?: boolean
}

export function BlurImage({
  src,
  alt,
  className = '',
  alreadyBlurred = false,
}: BlurImageProps) {
  return (
    <div
      className={`w-20 h-20 overflow-hidden rounded-lg ${className}`}
    >
      <img
        src={src}
        alt={alt}
        className={`w-full h-full object-cover ${alreadyBlurred ? '' : 'blur-md'}`}
      />
    </div>
  )
}
