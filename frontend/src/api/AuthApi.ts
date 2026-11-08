import type { ApiResponse } from '../types/ApiResponse'
import type { LoginRequest, LoginResponse } from '../types/Auth'

export class LoginError extends Error {
  readonly code: string

  constructor(code: string, message: string) {
    super(message)
    this.name = 'LoginError'
    this.code = code
  }
}

export async function login(
  request: LoginRequest,
): Promise<LoginResponse> {
  let response: Response

  try {
    response = await fetch('/api/users/login', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })
  } catch {
    throw new LoginError(
      'NETWORK_ERROR',
      '서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }

  const body: ApiResponse<LoginResponse> | null =
    await response.json().catch(() => null)

  if (!response.ok || !body || !body.success) {
    if (body?.success === false) {
      throw new LoginError(
        body.error.code,
        body.error.message,
      )
    }

    throw new LoginError(
      'LOGIN_FAILED',
      '로그인하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }

  if (!body.data?.accessToken || body.data.tokenType !== 'Bearer') {
    throw new LoginError(
      'INVALID_RESPONSE',
      '로그인 응답을 확인하지 못했어요. 다시 시도해 주세요.',
    )
  }

  return body.data
}