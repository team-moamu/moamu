
import defaultProfileMu from '../../../assets/images/default_profile_mu.png'

export function Avatar() {
  return (
    <img
      src={defaultProfileMu}
      alt="프로필 이미지"
      className="h-10 w-10 rounded-full object-cover"
    />
  )
}