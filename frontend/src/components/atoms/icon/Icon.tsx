// 이 파일은 폴더 구조 구성을 위한 임시 파일입니다. 첫 수정자는 이 주석을 삭제한 후 사용해 주세요.
type IconProps = { label: string }

export function Icon({ label }: IconProps) {
  return <span role="img" aria-label={label}>◯</span>
}
