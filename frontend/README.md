# MOAMU Frontend

MOAMU(모아뮤)는 습관을 실천하며 명화 퍼즐을 완성하는 서비스입니다. 프론트엔드는 **React + TypeScript + Vite + Tailwind CSS v4**를 사용하며, 반응형 웹을 기반으로 개발합니다.

현재는 개발 환경과 **Atomic Design 기반 초기 폴더 구조**를 마련한 상태입니다. 일부 `.tsx` 파일은 구조를 보여주기 위한 임시 구현이며, 실제 기능과 백엔드 연동은 개발 과정에서 추가합니다.

## 1. 개발 환경

- Node.js **24.22.1** (`.nvmrc` 기준)
- npm **11.19.1** (`packageManager` 설정 기준)
- React + TypeScript + Vite
- Tailwind CSS v4
- 백엔드(Java/Spring Boot)와 독립적으로 실행

> **버전 확인:** 실제 설치 패키지의 정확한 버전은 `package.json`과 `package-lock.json`을 기준으로 합니다. Node.js와 npm 버전은 아래 명령으로 확인합니다.

```sh
node -v
npm -v
```

이미 `v24.22.1`, `11.19.1`이라면 다음 설치 과정을 건너뛰어도 됩니다.

## 2. Node.js 설치

운영체제에 맞는 방법 **하나만** 사용합니다. 아래 설치 명령은 프로젝트 폴더 밖에서도 실행할 수 있습니다.

### macOS — nvm 사용

기존에 nvm을 사용 중이라면 설치 스크립트를 다시 실행하지 말고 `nvm install` 단계부터 진행합니다. 새로운 설치 스크립트를 실행하기 전에 [nvm 공식 안내](https://github.com/nvm-sh/nvm#installing-and-updating)를 확인하세요.

```sh
# 기본 zsh 설정 파일 준비 (기존 내용 유지)
touch ~/.zshrc

# nvm 공식 설치 스크립트 (프로젝트에서 안내한 버전)
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.8/install.sh | bash

# 현재 터미널에서 nvm 로딩
export NVM_DIR="$HOME/.nvm"
. "$NVM_DIR/nvm.sh"

# 팀 지정 버전 설치 및 선택
nvm install 24.22.1
nvm use 24.22.1

node -v
npm -v
```

새 터미널에서 `nvm`을 찾지 못하면 설치 로그와 셸 설정을 확인하고 `source ~/.zshrc`를 실행합니다. 이후 `frontend/`에서는 `.nvmrc`를 이용할 수 있습니다.

```sh
nvm install
nvm use
```

> `nvm install 24`처럼 메이저 버전만 지정하면 실행 시점에 따라 다른 패치 버전이 설치될 수 있으므로 `.nvmrc`를 사용해 주세요.

### Windows — 공식 설치 파일 사용

macOS용 nvm 설치 스크립트는 Windows PowerShell에서 실행하지 않습니다.

1. [Node.js 공식 다운로드](https://nodejs.org/en/download)에서 **24.22.1** 버전과 **Windows Installer (.msi)**를 선택합니다.
2. 일반 Intel/AMD Windows PC는 **x64**, ARM 기반 PC는 **ARM64**를 선택합니다. 시스템 종류는 **설정 → 시스템 → 정보**에서 확인할 수 있습니다.
3. 설치 파일을 실행하고 npm과 PATH 등록을 포함한 기본 구성으로 설치합니다.
4. VS Code를 완전히 종료한 뒤 다시 열고 새 PowerShell 터미널에서 확인합니다.

```powershell
node -v
npm.cmd -v
```

PowerShell 실행 정책으로 `npm.ps1`이 차단되는 경우를 고려하여, 이 문서의 Windows 명령은 `npm.cmd`로 표기합니다. Windows 설치 파일 방식에서는 `.nvmrc`가 자동으로 Node.js 버전을 변경하지 않습니다. 이미 다른 Node.js 버전 관리 도구를 사용한다면 설치 방식을 중복하지 마세요.

### npm 버전이 다른 경우

먼저 `node -v`가 `v24.22.1`인지 확인한 후 필요한 경우에만 버전을 맞춥니다.

**macOS**

```sh
npm install -g npm@11.19.1
npm -v
```

**Windows PowerShell**

```powershell
npm.cmd install -g npm@11.19.1
npm.cmd -v
```

## 3. 실행 방법

아래 명령은 **저장소 루트(`moamu/`)**에서 시작합니다.

### macOS

```sh
cd frontend
npm ci
npm run dev
```

### Windows PowerShell

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

터미널에 표시되는 개발 서버 주소를 브라우저에서 엽니다. 기본적으로 `http://localhost:5173`을 사용하며, 포트가 점유되어 있으면 달라질 수 있습니다. 개발 중 코드를 저장하면 화면에 변경 사항이 반영됩니다.

### 확인 명령

```sh
npm run lint
npm run build
npm run preview
```

Windows PowerShell에서는 `npm`을 `npm.cmd`로 바꿔 실행합니다. `lint`는 린트 검사, `build`는 프로젝트에 설정된 타입 검사·번들 빌드, `preview`는 빌드 결과 확인에 사용합니다. **실제 사용 가능한 스크립트는 `package.json`의 `scripts`를 기준으로 확인**하세요.

- `package.json`, `package-lock.json`: Git에 포함
- `node_modules/`, `dist/`: Git에서 제외
- 기존 프로젝트 의존성 설치: `npm ci`
- 새 라이브러리 추가: `npm install` 실행 후 `package.json`과 `package-lock.json`을 함께 커밋

## 4. 폴더 구조

```text
frontend/
├── public/                     # 공개 정적 파일, PWA 리소스(추후)
├── src/
│   ├── assets/                 # 이미지·아이콘·폰트
│   ├── components/             # 공통 UI (Atomic Design)
│   │   ├── atoms/              # Button, Input 등 기본 요소
│   │   ├── molecules/          # form, card, modal 등 조합 요소
│   │   ├── organisms/          # 습관 목록, 퍼즐 보드 등 복합 영역
│   │   └── layout/             # 헤더, 하단 내비게이션 등 레이아웃
│   ├── pages/                  # 실제 화면(홈, 습관, 미술관 등)
│   ├── routes/                 # URL 및 라우팅 설정
│   ├── api/                    # 공통 API 클라이언트·요청 설정
│   ├── services/               # 백엔드 도메인별 API 호출
│   ├── hooks/                  # 재사용 가능한 React 커스텀 훅
│   ├── stores/                 # 공통으로 공유하는 상태
│   ├── types/                  # 공통 타입·인터페이스
│   ├── constants/              # 여러 곳에서 사용하는 상수
│   ├── utils/                  # 공통 유틸리티 함수
│   ├── styles/                 # 전역 스타일·디자인 토큰
│   ├── App.tsx                 # 앱 최상위 컴포넌트
│   └── main.tsx                # 앱 진입점
├── .nvmrc                      # Node.js 버전
├── package.json                # 프로젝트 설정·패키지 목록
├── package-lock.json           # 고정된 의존성 버전
├── vite.config.ts              # Vite·Tailwind 설정
└── README.md
```

> 폴더는 현재 구성된 초기 구조를 설명합니다. 아직 구현되지 않은 기능은 `.gitkeep` 또는 임시 코드만 있을 수 있습니다. 폴더나 파일이 존재한다고 해당 기능이 완료된 것은 아닙니다.

### 주요 디렉터리 사용 기준

| 경로 | 역할 | 예시 |
|---|---|---|
| `components/` | 여러 화면에서 재사용하는 UI | Button, HabitCard |
| `pages/` | 라우트에 대응하는 화면 | HomePage, GalleryPage |
| `routes/` | URL과 페이지 연결, 접근 제어 | 라우트 설정 |
| `assets/` | 이미지·아이콘·폰트 등의 에셋 | 캐릭터 뮤 이미지 |
| `styles/` | 공통 CSS 및 디자인 토큰 | 색상, 폰트 스타일 |
| `constants/` | 여러 기능에서 공유하는 고정값 | 페이지 경로 상수 |
| `utils/` | UI에 종속되지 않는 공통 함수 | 날짜 표시 함수 |
| `hooks/` | 재사용할 React 로직 | 공통 커스텀 훅 |
| `api/` | HTTP 클라이언트와 공통 요청/응답 처리 | API 클라이언트 |
| `services/` | 도메인별 서버 API 호출 함수 | `user/`, `challenge/`, `verification/` |
| `stores/` | 여러 화면에서 공유하는 클라이언트 상태 | 사용자 UI 상태 |
| `types/` | 공통 TypeScript 타입 | API 공통 응답 타입 |

백엔드의 `user`, `ai`, `artwork`, `challenge`, `goal`, `media`, `museum`, `puzzle`, `schedule`, `verification` 등 도메인과 대응하는 API 파일은 필요해질 때 `services/`에 작성합니다. **백엔드 도메인과 프론트 페이지를 반드시 1:1로 맞추지는 않습니다.** API 계약은 백엔드 명세를 확인한 후 반영합니다.

## 5. Atomic Design 분류 기준

`src/components/`는 **Atoms → Molecules → Organisms** 기준으로 구분하며, 공통 화면 배치는 `layout/`에서 관리합니다.

| 계층 | 기준 | 모아뮤 예시 |
|---|---|---|
| `atoms/` | 독립적인 기본 UI 요소 | Button, Input, Icon, Badge |
| `molecules/` | 기본 UI 요소를 조합한 작은 기능 단위 | InputField, HabitCard, ConfirmDialog |
| `organisms/` | 여러 요소가 결합된 복합 UI 영역 | HabitList, PuzzleBoard, GalleryGrid |
| `layout/` | 페이지 간 공통 배치 요소 | MainHeader, BottomNavigation |

예를 들어 `Input`과 `Button`을 조합해 입력 기능을 구현한다면 `molecules/form/`에 배치할 수 있습니다. 특정 페이지에서만 쓰이고 재사용 가능성이 낮은 UI는 무조건 공통 폴더로 이동하지 않고 해당 `pages/` 가까이에 둘 수 있습니다.

### 컴포넌트 작성 규칙

- 컴포넌트 파일명은 **PascalCase**를 사용합니다. 예: `HabitCard.tsx`
- 먼저 기존 컴포넌트의 재사용 가능 여부를 확인합니다.
- 공통 UI는 `components/`, 페이지 전용 UI는 해당 페이지 가까이에 작성합니다.
- Atomic Design 계층은 단순히 코드 길이가 아니라 **UI 책임과 조합 관계**를 기준으로 판단합니다.
- 새로운 공통 컴포넌트 구조가 필요한 경우 팀원들과 공유한 뒤 추가합니다.

## 6. 임시 파일 관리 규칙

### 임시 `.tsx` 파일

초기 구조를 위해 생성한 `.tsx` 파일에는 다음과 같은 첫 줄 주석이 있습니다.

```tsx
// 이 파일은 폴더 구조 구성을 위한 임시 파일입니다. 첫 수정자는 이 주석을 삭제한 후 사용해 주세요.
```

**실제 구현을 시작하는 담당자는** 임시 주석을 삭제하고 코드가 요구사항에 맞는지 확인한 후 수정·교체해 주세요. 주석을 삭제했다는 사실만으로 기능 구현이 완료된 것은 아닙니다.

### `.gitkeep` 파일

Git은 빈 디렉터리만 별도로 추적하지 않기 때문에, 비어 있는 폴더에 `.gitkeep`을 넣었습니다.

- 실제 `.ts`, `.tsx` 등 파일이 추가되면 **그 폴더의 `.gitkeep`을 삭제**합니다.
- 아직 파일이 없으면 `.gitkeep`을 유지합니다.
- 삭제한 `.gitkeep`도 일반 파일 변경처럼 커밋에 포함합니다.

## 7. API·상태 관리 규칙

챌린지 생성 화면은 `POST /api/challenges`에 `goalTemplateId`, `startDate`(`YYYY-MM-DD`), `durationDays`(7/14/30)를 전송합니다. 개발 시 Vite가 `/api` 요청을 `http://localhost:8080`으로 전달하므로 백엔드도 실행해야 합니다. 배포 환경에서는 같은 출처의 `/api`가 백엔드로 연결되도록 설정해야 합니다. 현재 목표 목록은 임시 데이터이며, 해당 ID의 목표 템플릿과 연결 작품이 DB에 있어야 생성됩니다.

- `api/`: 공통 API 클라이언트, 기본 URL, 요청·응답 처리 등 **통신 공통부**를 관리합니다.
- `services/`: 백엔드 도메인별로 API 호출 함수를 관리합니다.
- `hooks/`: UI에서 반복되는 React 로직을 커스텀 훅으로 분리합니다.
- `stores/`: 여러 컴포넌트에서 실제로 공유해야 하는 클라이언트 상태만 관리합니다.
- `types/`: 둘 이상의 모듈에서 사용하는 타입을 배치합니다. 한 컴포넌트에서만 쓰는 타입은 그 근처에 둡니다.
- API 응답 형태, 인증 방식, 오류 코드 등은 임의로 확정하지 않고 **백엔드 API 명세와 합의한 뒤** 적용합니다.

## 8. Tailwind CSS 및 스타일 규칙

- **Tailwind CSS v4**를 기본 스타일링 방식으로 사용합니다.
- 전역 CSS에서 `@import "tailwindcss";`가 적용되어 있으면 각 `.tsx` 파일마다 Tailwind를 import할 필요가 없습니다.
- 컴포넌트에서는 `className`을 이용합니다.
- 공통 색상·폰트·간격 등 디자인 토큰은 팀의 스타일 가이드에 맞춰 한곳에서 관리합니다.
- 중복 스타일이 많아지면 공통 컴포넌트로 추출할지 검토합니다.
- 모바일과 데스크톱 화면에서 레이아웃이 자연스럽게 동작하도록 반응형을 고려합니다.

## 9. Git 협업 규칙

- **커밋 메시지 설명은 한국어**로 작성합니다. 예: `feat: 습관 등록 화면 구현`
- `feat`, `fix`, `chore`, `docs` 등 작업 유형 접두사를 사용합니다.
- 하나의 커밋에는 논리적으로 관련된 변경사항을 담습니다.
- PR에는 **변경 사항**과 **참고 사항**을 중심으로 작성하고 필요한 경우 테스트 결과를 추가합니다.
- PR 전에는 해당 프로젝트에서 지원하는 `lint`, `build` 명령 등을 확인합니다.
- 폴더 구조나 공통 규칙을 바꿀 때에는 팀원과 공유합니다.

## 10. 현재 구현 범위 및 주의 사항

- 프론트엔드 개발·빌드 환경과 초기 폴더 구조를 구성했습니다.
- 예시 컴포넌트가 있어도 실제 서비스 기능이 구현되었다는 의미는 아닙니다.
- 백엔드 API 연결, 인증, 라우팅, PWA 설치·서비스 워커 등은 **구현·검증 상태를 별도로 확인**해야 합니다.
- 반응형 화면은 기능 개발 시 모바일 화면 폭을 포함해 검증합니다.
- 프론트엔드에 DB 비밀번호, JWT 서명 비밀키 등 **서버 비밀정보를 넣지 않습니다**. `VITE_`로 시작하는 환경변수도 클라이언트 번들에 포함될 수 있으므로 공개 가능한 값만 사용합니다.
