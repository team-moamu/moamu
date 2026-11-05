type BlurImageProps = {
  src: string
  alt: string
  className?: string
}

export function BlurImage({
  src,
  alt,
  className = '',
}: BlurImageProps) {
  return (
    <div
      className={`w-20 h-20 overflow-hidden rounded-lg ${className}`}
    >
      <img
        src={src}
        alt={alt}
        className="w-full h-full object-cover blur-md"
      />
    </div>
  )
}