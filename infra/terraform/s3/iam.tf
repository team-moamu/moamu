# 팀원 이름이 정해지면 적용 전에 이 네 이름을 바꿔 주세요.
locals {
  backend_users = toset([
    "moamu-backend-1",
    "moamu-backend-2",
    "moamu-backend-3",
    "moamu-backend-4",
  ])
}

# 팀원별 IAM 사용자 4명
resource "aws_iam_user" "backend" {
  for_each = local.backend_users
  name     = each.value
}

# 네 사용자에게 같은 S3 권한을 주기 위한 그룹
resource "aws_iam_group" "backend" {
  name = "moamu-backend-s3"
}

resource "aws_iam_user_group_membership" "backend" {
  for_each = aws_iam_user.backend

  user   = each.value.name
  groups = [aws_iam_group.backend.name]
}

resource "aws_iam_group_policy" "backend_s3" {
  name  = "moamu-media-access"
  group = aws_iam_group.backend.name

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        # S3 콘솔에 버킷 목록을 표시하는 데 필요합니다.
        # 다른 버킷의 이름은 보일 수 있지만, 파일 접근 권한은 주지 않습니다.
        Effect   = "Allow"
        Action   = ["s3:ListAllMyBuckets"]
        Resource = "*"
      },
      {
        # 우리 버킷 안의 파일 목록과 리전 확인
        Effect = "Allow"
        Action = [
          "s3:ListBucket",
          "s3:GetBucketLocation",
        ]
        Resource = aws_s3_bucket.media.arn
      },
      {
        # 우리 버킷의 파일 읽기와 업로드만 허용
        Effect = "Allow"
        Action = [
          "s3:GetObject",
          "s3:PutObject",
        ]
        Resource = "${aws_s3_bucket.media.arn}/*"
      },
    ]
  })
}

# 팀원이 첫 로그인 때 임시 비밀번호를 본인 비밀번호로 바꿀 수 있게 합니다.
# 자기 계정의 비밀번호 변경만 허용하며 다른 IAM 관리 권한은 주지 않습니다.
resource "aws_iam_group_policy_attachment" "backend_change_password" {
  group      = aws_iam_group.backend.name
  policy_arn = "arn:aws:iam::aws:policy/IAMUserChangePassword"
}
