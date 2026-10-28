# Terraform이 사용할 플러그인(provider)을 선언
# provider는 Terraform 설정을 AWS API 호출로 바꿔 주는 구성 요소
terraform {
  required_providers {
    aws = {
      # AWS 공식 Terraform provider를 사용
      source = "hashicorp/aws"
    }
  }
}

# 아래에서 선언하는 모든 AWS 자원에 적용할 접속 설정
provider "aws" {
  # S3 버킷을 서울 리전에 생성, 추후 리전변경은 불가능
  region = "ap-northeast-2"

  # AWS 계정은 실행하는 사람이 AWS_PROFILE 환경변수로 선택합니다.
  # 예: AWS_PROFILE=moamu terraform plan
  # 개인별 프로필 이름을 코드에 고정하지 않으면 팀원이 같은 파일을 사용할 수 있습니다.
}

# 사진과 생성 작품을 저장할 S3 버킷
resource "aws_s3_bucket" "media" {
  bucket = "moamu-s3-bucket"
}

# 버킷과 객체를 실수로 인터넷에 공개하지 않도록 차단
# 사진을 사용자에게 제공할 때는 공개 설정 대신 서명 URL 등을 사용
resource "aws_s3_bucket_public_access_block" "media" {
  # 위에서 만든 버킷을 참조 Terraform이 생성 순서도 자동으로 판단
  bucket = aws_s3_bucket.media.id

  # 새 공개 ACL 설정을 거부
  block_public_acls = true

  # 공개 접근을 허용하는 새 버킷 정책을 거부
  block_public_policy = true

  # 기존 또는 새 객체에 설정된 공개 ACL을 무시
  ignore_public_acls = true

  # 공개 버킷 정책을 통한 접근을 제한
  restrict_public_buckets = true
}
