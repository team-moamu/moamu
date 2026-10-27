# 백엔드 팀 S3 사용 안내

## 현재 구성

- S3 버킷: `moamu-s3-bucket`
- 리전: `ap-northeast-2` (서울)
- 버킷은 비공개이며, 퍼블릭 액세스가 차단되어 있습니다.
- 백엔드 팀원용 IAM 사용자 4명에게 **이 버킷의 파일 목록·읽기·업로드 권한**을 부여했습니다. 파일 삭제 권한은 없습니다.

| 팀원별 IAM 사용자 |
| --- |
| `moamu-backend-1` |
| `moamu-backend-2` |
| `moamu-backend-3` |
| `moamu-backend-4` |

각 팀원은 배정받은 **자기 계정의 Access key ID와 Secret access key**를 로컬 개발 환경에 설정합니다. 키 값과 콘솔 비밀번호는 각자에게 별도로 전달하며 Git에 올리지 않습니다.

## 로컬 Spring 설정

각자의 로컬 실행 환경에 다음 값을 설정합니다. `.env` 파일을 쓴다면 프로젝트의 로더나 IDE 설정으로 **프로세스 환경변수에 실제로 전달**해야 합니다. Spring과 AWS SDK가 `.env` 파일을 자동으로 읽는 것은 아닙니다. 아래의 `...`는 **본인 키**로 바꿉니다.

```dotenv
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=ap-northeast-2
S3_BUCKET=moamu-s3-bucket
```

Spring의 `application.yml`에는 버킷·리전을 다음처럼 연결할 수 있습니다.

```yaml
app:
  s3:
    bucket: ${S3_BUCKET:moamu-s3-bucket}
    region: ${AWS_REGION:ap-northeast-2}
```

AWS SDK for Java 2.x의 S3 클라이언트 또는 `S3Presigner`에서 이 값을 사용합니다. 자격 증명은 SDK의 기본 자격 증명 체인이 위 환경변수에서 읽도록 합니다. **코드에 키를 직접 적지 않습니다.**

사진 업로드는 Spring이 사용자 권한을 확인하고 객체 키를 만든 뒤 **업로드용 서명 URL**을 발급하는 흐름을 권장합니다. 브라우저가 이 URL로 S3에 사진을 올리고, Spring은 업로드 완료를 확인한 뒤 DB에 **객체 키**를 저장합니다. 사진 조회 시에도 Spring이 권한을 확인하고 조회용 서명 URL을 발급합니다. 서명 URL 자체는 DB에 저장하지 않습니다.

## 아직 필요한 설정

- **브라우저에서 S3로 직접 업로드한다면 CORS 설정이 필요합니다.** 현재 버킷에는 CORS가 없으므로 프런트엔드 주소가 정해지면 인프라 담당자에게 알려주세요.
- Kubernetes에 배포하는 Spring 서버에는 **팀원 개인 키 대신 서버 전용 자격 증명**을 사용해야 합니다.

## Git에 올리는 파일

`main.tf`, `iam.tf`, `.terraform.lock.hcl`, 이 `README.md`, `.gitignore`는 공유할 수 있습니다. `terraform.tfstate`, `.terraform/`, `.env`, 액세스 키, 비밀번호는 올리지 않습니다. Terraform 적용은 현재 상태 파일을 관리하는 인프라 담당자만 진행합니다.
