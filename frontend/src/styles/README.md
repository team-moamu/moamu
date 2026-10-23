# 모아뮤 공통 스타일

[Figma 스타일 가이드](https://www.figma.com/design/cDUSAxjfzKUxxFA3u7zv8e?node-id=27-2)의 핵심 색상, 글자 크기, 간격, 모서리, 버튼·입력창 규격을 옮긴 CSS입니다. [전체 와이어프레임](https://www.figma.com/design/cDUSAxjfzKUxxFA3u7zv8e?node-id=13-3)의 개별 화면이나 React 컴포넌트는 포함하지 않습니다.

## 파일 역할

| 파일 | 수정할 내용 |
| --- | --- |
| `tokens.css` | 색상·글꼴·간격·모서리 등 공통 값 |
| `base.css` | 전체 페이지 기본 스타일, 키보드 포커스 |
| `typography.css` | 제목·본문·설명용 글자 스타일 |
| `components.css` | 버튼·입력창·카드·아이콘 클래스 |
| `index.css` | 위 CSS를 한 번에 불러오는 진입점 |

이미 `main.tsx → src/index.css → styles/index.css`로 연결했습니다. 각 TSX에서 다시 import할 필요 없이 `className`으로 사용합니다. Tailwind 설치는 필요하지 않습니다.

## 바로 사용하기

```tsx
<section className="card">
  <h2 className="text-heading">내 미술관</h2>
  <p className="text-body">작은 실천으로 작품을 완성해 보세요.</p>
  <p className="text-caption text-muted">완성한 작품이 여기에 표시돼요.</p>
  <button type="button" className="button button--primary">
    도전 시작하기
  </button>
</section>
```

버튼 변형은 `button--primary`(52px), `button--secondary`(44px), `button--text`입니다. 가로 전체 너비는 `button--full`을 추가합니다. 긴 문구·글자 확대 시 잘리지 않도록 높이는 `min-height`입니다. 비활성화는 `<button disabled>`로 지정합니다. 스타일만으로 클릭을 막지는 않습니다.

```tsx
<div className="field">
  <label className="field__label" htmlFor="challenge-name">도전 이름</label>
  <input
    className="input"
    id="challenge-name"
    placeholder="매일 책과 독서 기록 남기기"
    aria-describedby="challenge-name-hint"
  />
  <p className="field__hint" id="challenge-name-hint">나만의 목표를 적어 주세요.</p>
</div>
```

오류가 있으면 `aria-invalid="true"`와 `field__error` 안내문을 함께 사용하고, `aria-describedby`를 오류 안내문의 ID와 연결합니다. `textarea`, `select`에도 `input` 클래스를 사용할 수 있습니다. 상태 안내는 색상 외에도 텍스트·아이콘을 함께 제공합니다.

## 각자 만드는 화면 CSS

공통 값은 변수로 가져오고, 기능에만 쓰는 배치는 해당 기능의 CSS 또는 CSS Module에 작성하세요. 모든 화면의 클래스를 이 폴더에 모을 필요는 없습니다.

```css
.artworkList {
  display: grid;
  gap: var(--space-4);
  padding: var(--page-padding);
  color: var(--color-text);
  background-color: var(--color-background);
}
```

- 색상: `--color-primary`(Forest), `--color-background`(Cream), `--color-text`(Cocoa), `--color-surface-soft`, `--color-border`, `--color-text-muted`
- 간격: `--space-1/2/3/4/6/8/12` = 4/8/12/16/24/32/48px
- 모서리: `--radius-sm/md/lg/xl` = 8/12/20/28px, 입력창은 `--radius-input` = 10px
- 글자: `text-title`, `text-heading`, `text-accent`, `text-description`, `text-body`, `text-label`, `text-caption`
- `h1`, `h2`에는 기본 제목 스타일이 적용됩니다. 다른 외형이 필요하면 위 클래스로 지정하세요.

Hover/pressed 색상은 기본 Forest 색에서 어둡게 파생한 구현값입니다. 흰색 표면, 비활성화 외형과 포커스 외곽선도 공통 사용을 위한 구현값이며, Figma의 모든 상태를 정확한 수치로 추출한 것은 아닙니다. 가이드의 Reward/Pending 보조 색상은 원본 이미지에서 HEX 값을 명확히 판독하지 못해 임의 추가하지 않았습니다.

## 글꼴

| 용도 | 가이드 글꼴 | 클래스 |
| --- | --- | --- |
| 제목 | 학교안심 둥근미소 Regular/Bold | `text-title`, `text-heading` |
| 강조 문구 | 그리운 예은체 / Griun Yeeun | `text-accent` |
| 설명 | 강원교육모두체 / GangwonEduAll BoldA | `text-description` |
| 본문·UI | Pretendard Regular/Medium/SemiBold | `text-body`, `text-label`, `text-caption` |

**현재 저장소에 글꼴 파일이 없어 웹폰트 로딩은 아직 연결하지 않았습니다.** CSS의 `font-family`는 글꼴을 설치하거나 다운로드하는 기능이 아닙니다. 해당 글꼴이 없는 환경에서는 시스템 글꼴로 표시됩니다.

팀에서 사용할 웹폰트 파일과 라이선스를 준비한 뒤 이 폴더에 `fonts.css`를 추가하고 `index.css`에서 import하면 됩니다. `@font-face`의 이름을 `tokens.css`의 family 이름과 맞추고, 굵기별 파일을 연결하세요. PC에만 설치된 글꼴에 의존하면 팀원과 배포 환경의 화면이 달라질 수 있습니다.

현재 초기 화면의 너비·바깥 여백은 `src/index.css`에 남아 있습니다. 화면 구현을 시작할 때 해당 `main` 규칙을 실제 앱 레이아웃으로 옮기세요. 공통 스타일에는 모바일 화면 폭이나 하단 내비게이션 위치를 고정하지 않았습니다.
