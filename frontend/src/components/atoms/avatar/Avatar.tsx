import defaultProfileMu from '../../../assets/images/default_profile_mu.png'

type AvatarProps = {
  profilePhotoUrl?: string | null
}

export function Avatar({ profilePhotoUrl }: AvatarProps) {
  return (
    <img
      src={profilePhotoUrl ?? defaultProfileMu}
      alt="프로필 이미지"
      className="h-10 w-10 rounded-full object-cover"
    />
  )
}