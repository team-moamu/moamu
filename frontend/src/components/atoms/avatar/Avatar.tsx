// 이 파일은 폴더 구조 구성을 위한 임시 파일입니다. 첫 수정자는 이 주석을 삭제한 후 사용해 주세요.
type AvatarProps = { name: string }

export function Avatar({ name }: AvatarProps) {
  return <span aria-label={`${name} 프로필`}>{name.slice(0, 1)}</span>
}
