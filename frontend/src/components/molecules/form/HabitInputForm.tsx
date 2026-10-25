// 이 파일은 폴더 구조 구성을 위한 임시 파일입니다. 첫 수정자는 이 주석을 삭제한 후 사용해 주세요.
import { Button } from '../../atoms/button/Button'
import { Input } from '../../atoms/input/Input'

export function HabitInputForm() {
  return <form onSubmit={(event) => event.preventDefault()}><Input aria-label="습관 이름" placeholder="습관 이름" /><Button type="submit">추가</Button></form>
}
