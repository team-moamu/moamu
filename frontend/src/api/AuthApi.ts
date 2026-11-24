import type { ApiResponse } from '../types/ApiResponse'
import type {
  LoginRequest,
  LoginResponse,
  SignupRequest,
  SignupResponse,
  EmailAvailabilityResponse,
} from '../types/Auth'
import { authenticatedFetch } from './AuthenticatedFetch'

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

export class SignupError extends Error {
  readonly code: string

  constructor(code: string, message: string) {
    super(message)
    this.name = 'SignupError'
    this.code = code
  }
}

export async function signup(
  request: SignupRequest,
): Promise<SignupResponse> {
  let response: Response

  try {
    response = await fetch('/api/users/signup', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    })
  } catch {
    throw new SignupError(
      'NETWORK_ERROR',
      '서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }

  const body: ApiResponse<SignupResponse> | null =
    await response.json().catch(() => null)

  if (!response.ok || !body || !body.success) {
    if (body?.success === false) {
      throw new SignupError(
        body.error.code,
        body.error.message,
      )
    }

    throw new SignupError(
      'SIGNUP_FAILED',
      '회원가입하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }

  if (!Number.isSafeInteger(body.data?.userId) || body.data.userId <= 0) {
    throw new SignupError(
      'INVALID_RESPONSE',
      '회원가입 결과를 확인하지 못했어요. 로그인 화면에서 가입한 정보로 로그인을 시도해 주세요.',
    )
  }

  return body.data
}

export async function checkEmailAvailability(
  email: string,
): Promise<EmailAvailabilityResponse> {
  let response: Response

  try {
    response = await fetch(
      `/api/users/email-availability?${new URLSearchParams({ email })}`,
    )
  } catch {
    throw new SignupError(
      'NETWORK_ERROR',
      '서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }

  const body: ApiResponse<EmailAvailabilityResponse> | null =
    await response.json().catch(() => null)

  if (!response.ok || !body || !body.success) {
    if (body?.success === false) {
      throw new SignupError(body.error.code, body.error.message)
    }

    throw new SignupError(
      'EMAIL_CHECK_FAILED',
      '이메일 중복 확인에 실패했어요. 잠시 후 다시 시도해 주세요.',
    )
  }

  if (typeof body.data?.available !== 'boolean') {
    throw new SignupError(
      'INVALID_RESPONSE',
      '이메일 중복 확인 결과를 확인하지 못했어요.',
    )
  }

  return body.data
}
async function postEmailVerification(
  path: '' | '/verify',
  request: { email: string; code?: string },
): Promise<Response> {
  let response: Response

  try {
    response = await fetch(`/api/users/email-verifications${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    })
  } catch {
    throw new SignupError(
      'NETWORK_ERROR',
      '서버에 연결하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }

  if (!response.ok) {
    const body: ApiResponse<never> | null =
      await response.json().catch(() => null)

    if (body?.success === false) {
      throw new SignupError(body.error.code, body.error.message)
    }

    throw new SignupError(
      'EMAIL_VERIFICATION_FAILED',
      '이메일 인증 요청에 실패했어요. 다시 시도해 주세요.',
    )
  }

  return response
}

export async function sendEmailVerification(email: string): Promise<void> {
  await postEmailVerification('', { email })
}

export async function verifyEmailCode(
  email: string,
  code: string,
): Promise<string> {
  const response = await postEmailVerification('/verify', { email, code })
  const body: ApiResponse<{ verificationToken: string }> | null =
    await response.json().catch(() => null)

  if (
    body?.success !== true ||
    typeof body.data?.verificationToken !== 'string' ||
    !/^[A-Za-z0-9_-]{43}$/.test(body.data.verificationToken)
  ) {
    throw new SignupError(
      'INVALID_RESPONSE',
      '이메일 인증 결과를 확인하지 못했어요. 인증번호를 다시 요청해 주세요.',
    )
  }

  return body.data.verificationToken
}

export async function logout(): Promise<void> {
  const response = await fetch('/api/users/logout', {
    method: 'POST',
    credentials: 'same-origin',
  })

  if (!response.ok) {
    throw new Error('로그아웃하지 못했어요. 잠시 후 다시 시도해 주세요.')
  }
}
type UserMeResponse = {
  email: string | null
  name: string
}

export async function getMe(signal?: AbortSignal): Promise<UserMeResponse> {
  const response = await authenticatedFetch('/api/users/me', { signal })
  const body: ApiResponse<UserMeResponse> | null =
    await response.json().catch(() => null)

  if (!response.ok || !body?.success) {
    throw new Error('회원 정보를 불러오지 못했어요.')
  }

  return body.data
}