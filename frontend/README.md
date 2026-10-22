# MOAMU Frontend

React + TypeScript + Vite 기본 프로젝트입니다. 기능별 폴더는 구현할 때 추가합니다.

## 개발 환경

- Node.js 24.22.1 LTS (`.nvmrc`: 24.22.1)
- npm 11.19.1 (Node.js 24.22.1에 포함, `packageManager`: npm@11.19.1)
- Java/Spring Boot 백엔드와 독립적으로 실행합니다.

## Node.js 설치

이미 `node -v`가 `v24.22.1`, `npm -v`가 `11.19.1`이면 설치를 건너뜁니다.
운영체제에 맞는 방법 하나만 사용하세요. 아래 설치 명령은 프로젝트 폴더 밖에서도 실행할 수 있습니다.

### macOS — nvm 사용

macOS 터미널 또는 VS Code의 zsh 터미널에서 아래 명령을 줄 단위로 실행합니다.
기존에 nvm을 사용한다면 설치 부분을 건너뛰고 Node.js 설치부터 진행하세요.

```sh
# 기본 zsh 설정 파일 준비 (기존 내용은 유지)
touch ~/.zshrc

# 공식 nvm 설치 스크립트 실행
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.8/install.sh | bash

# 현재 터미널에서 nvm 로딩
export NVM_DIR="$HOME/.nvm"
. "$NVM_DIR/nvm.sh"

# 팀에서 사용하는 정확한 버전 설치·선택
nvm install 24.22.1
nvm use 24.22.1

# 설치 확인
node -v
npm -v
```

출력은 각각 `v24.22.1`, `11.19.1`이어야 합니다.
`nvm install 24`는 실행 시점에 따라 다른 패치 버전을 설치할 수 있으므로 정확한 버전을 지정합니다.

새 터미널에서 `nvm`을 찾지 못하면 설치 로그의 셸 설정 안내를 확인하고,
zsh에서는 `source ~/.zshrc`를 실행합니다. 설치 중 개발자 도구 설치 안내가 나오면 완료한 뒤 다시 진행하세요.

이후 `frontend` 폴더에서는 `.nvmrc`를 이용할 수 있습니다.

```sh
nvm install
nvm use
```

[nvm 공식 설치 안내](https://github.com/nvm-sh/nvm#installing-and-updating)

### Windows — 공식 설치 파일 사용

위 macOS용 nvm 스크립트는 Windows PowerShell에서 실행하지 않습니다.

1. [Node.js 공식 다운로드](https://nodejs.org/en/download)에서 **24.22.1 LTS**를 선택합니다.
2. 운영체제를 **Windows**로 선택하고 **Windows Installer (.msi)**를 다운로드합니다.
   - 일반적인 Intel·AMD PC: **x64**
   - ARM 기반 Windows PC: **ARM64**
   - 시스템 종류는 Windows **설정 → 시스템 → 정보**에서 확인합니다.
3. 설치 파일을 실행하고 npm과 PATH 등록을 포함한 기본 구성으로 설치합니다.
4. VS Code를 완전히 종료한 뒤 다시 열고 새 PowerShell 터미널에서 확인합니다.

```powershell
node -v
npm.cmd -v
```

출력은 각각 `v24.22.1`, `11.19.1`이어야 합니다.
PowerShell 실행 정책으로 `npm.ps1` 실행이 차단되는 경우에도 사용할 수 있도록
이 문서의 Windows 명령은 `npm.cmd`로 표기합니다. 실행 정책을 변경할 필요는 없습니다.
Windows 설치 파일 방식에서는 `.nvmrc`가 버전을 자동으로 변경하지 않습니다.
이미 다른 버전 관리 도구를 사용한다면 설치 방식을 중복하지 말고 해당 도구에서 24.22.1을 선택하세요.

### npm 버전이 다른 경우

먼저 `node -v`가 `v24.22.1`인지 확인한 뒤 실행합니다.

```sh
# macOS
npm install -g npm@11.19.1
npm -v
```

```powershell
# Windows PowerShell
npm.cmd install -g npm@11.19.1
npm.cmd -v
```

## 실행

저장소 루트에서 실행합니다.

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

터미널에 표시되는 주소를 브라우저로 엽니다. 기본 주소는 http://localhost:5173 입니다.
코드를 저장하면 개발 화면에 변경 사항이 반영됩니다.

## 확인 명령

```sh
npm run lint
npm run build
npm run preview
```

Windows PowerShell에서는 위 명령의 `npm`을 `npm.cmd`로 바꿔 실행합니다.

`build`는 TypeScript 검사 후 배포 파일을 `dist`에 생성합니다.
`preview`는 빌드 결과를 로컬에서 확인하는 용도입니다.
`package-lock.json`은 Git에 포함하고, `node_modules`와 `dist`는 포함하지 않습니다.
새 라이브러리 추가 시에는 `npm install`을 사용하고 변경된 lock 파일도 공유합니다.

## 현재 범위

기본 화면과 개발·빌드 환경을 구성했습니다.
백엔드 API 연결, 라우팅, 인증, PWA(설치·서비스 워커)는 아직 구현하지 않았습니다.
반응형 화면은 각 기능 개발 시 모바일 화면 폭도 함께 확인합니다.
프론트에 DB 비밀번호, JWT 서명 비밀키 같은 서버 비밀정보를 넣지 않습니다.
