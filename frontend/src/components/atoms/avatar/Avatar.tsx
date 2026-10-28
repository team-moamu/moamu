interface AvatarProps {
  imageUrl?: string | null
}

export function Avatar({ imageUrl }: AvatarProps) {
  return (
    <img
      src={imageUrl || '../../assets/images/default_profile_mu.png'}
      alt="프로필 이미지"
      className="avatar"
    />
  )
}