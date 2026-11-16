# 데이터 안내

`backend/data/`는 서비스 초기 데이터 SQL과 실행 안내를 관리합니다. 테이블 구조는 [Flyway 마이그레이션](../src/main/resources/db/migration/)으로 생성·변경하고, 시드 SQL은 데이터만 입력합니다.

## 폴더 구성

```text
backend/data/
├── README.md
└── seedData/
    ├── README.md
    └── v8_seed-service.sql
```

## 초기 시드

현재 시드 파일은 [seedData/v8_seed-service.sql](seedData/v8_seed-service.sql)입니다. 서비스 카탈로그와 테스트 회원 5명을 생성합니다.

생성 데이터, 대상 스키마, 실행 전제 조건, Windows·macOS 실행 방법, S3 객체 키와 오류 처리는 [시드 실행 안내](seedData/README.md)를 따릅니다.

로컬 DB 준비가 필요하면 [프로젝트 환경 설정](../../README.md)과 [Docker DB 실행 안내](../../infra/README.md)를 참고합니다.
