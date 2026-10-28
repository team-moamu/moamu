# 백엔드 팀 S3 사용 안내

## 접속 정보

| 항목 | 값 |
| --- | --- |
| AWS 계정 ID | `539247467418` |
| 콘솔 로그인 주소 | https://539247467418.signin.aws.amazon.com/console |
| S3 버킷 | `moamu-s3-bucket` |
| 리전 | `ap-northeast-2` (서울) |

| IAM 사용자 | 담당자 |
| --- | --- |
| `moamu-backend-1` | 개별 안내 |
| `moamu-backend-2` | 개별 안내 |
| `moamu-backend-3` | 개별 안내 |
| `moamu-ai` | AI 담당자 |

본인에게 배정된 IAM 사용자 이름을 사용하세요. 버킷은 비공개이며, 위 사용자에게는 이 버킷의 **목록 조회, 파일 읽기, 업로드** 권한이 있습니다. 파일 삭제 권한은 없습니다.

## AWS 콘솔에서 확인하기

1. 위 **콘솔 로그인 주소**에 접속합니다. 네 사용자 모두 같은 주소를 사용합니다.
2. 본인의 **IAM 사용자 이름**과 별도로 전달받은 **임시 비밀번호**를 입력합니다.
3. 첫 로그인에서 요청하면 본인만의 새 비밀번호로 변경합니다.
4. AWS 콘솔에서 **S3 → `moamu-s3-bucket`**으로 이동합니다.

콘솔 로그인이 설정되지 않은 계정은 비밀번호를 받아도 로그인할 수 없습니다. 로그인이 안 되면 인프라 담당자에게 콘솔 액세스 설정 여부를 확인해 달라고 요청하세요. **Access Key ID와 Secret Access Key는 콘솔 로그인 비밀번호가 아닙니다.**

## 로컬 Spring 앱에서 사용하기

인프라 담당자에게 별도로 받은 **본인 계정의** Access Key ID와 Secret Access Key를 로컬 실행 환경변수로 설정합니다.

```dotenv
AWS_ACCESS_KEY_ID=<본인 Access Key ID>
AWS_SECRET_ACCESS_KEY=<본인 Secret Access Key>
AWS_REGION=ap-northeast-2
S3_BUCKET=moamu-s3-bucket
```

`.env` 파일을 쓴다면 실행 도구가 이 값을 **프로세스 환경변수로 전달하도록** 설정해야 합니다. Spring과 AWS SDK가 `.env` 파일을 자동으로 읽지는 않습니다. AWS SDK for Java의 기본 자격 증명 체인은 전달된 `AWS_ACCESS_KEY_ID`와 `AWS_SECRET_ACCESS_KEY`를 사용할 수 있습니다.

Spring 설정에서 버킷과 리전이 필요하면 다음처럼 참조할 수 있습니다.

```yaml
app:
  s3:
    bucket: ${S3_BUCKET:moamu-s3-bucket}
    region: ${AWS_REGION:ap-northeast-2}
```

S3의 `photos/` 같은 폴더는 **객체 키의 접두어**입니다. 미리 만들지 않아도 `photos/example.jpg`로 업로드하면 콘솔에 폴더처럼 표시됩니다. 사용할 경로 이름은 백엔드와 프런트엔드가 합의해 주세요.

**자격 증명과 콘솔 비밀번호는 코드, README, Git에 올리지 마세요.** 팀원별 값은 개별적으로 전달합니다. Kubernetes에 배포하는 서버에는 팀원 개인 키 대신 서버 전용 자격 증명을 사용합니다.
