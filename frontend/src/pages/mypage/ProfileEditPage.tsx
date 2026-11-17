import defaultProfileMu from '../../assets/images/default_profile_mu.png'
import { previewNickname } from './profilePreview'

export function ProfileEditPage() {
  return (
    <div className="space-y-6 px-6 py-6">
      <img
        src={defaultProfileMu}
        alt="기본 뮤 프로필"
        className="mx-auto h-24 w-24 rounded-full object-cover"
      />

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
    </div>
  )
}