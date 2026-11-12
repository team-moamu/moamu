import {
  clearAccessToken,
  getAccessToken,
  saveAccessToken,
} from '../auth/authStorage.ts'
import type { ApiResponse } from '../types/ApiResponse'
import type { LoginResponse } from '../types/Auth'

export class AuthenticationRequiredError extends Error {
  constructor() {
    super('다시 로그인해 주세요.')
    this.name = 'AuthenticationRequiredError'
  }
}

let refreshInFlight: Promise<string> | null = null
let lastRotation: { previous: string; current: string } | null = null

async function refreshAccessToken(previousToken: string): Promise<string> {
  const response = await fetch('/api/users/refresh', {
    method: 'POST',
    credentials: 'same-origin',
  })

  if (response.status === 401) {
    if (getAccessToken() === previousToken) {
      clearAccessToken()
      lastRotation = null

      if (window.location.pathname !== '/login') {
        window.location.replace('/login')
      }
    }

    throw new AuthenticationRequiredError()
  }

  if (!response.ok) {
    throw new Error('로그인 상태를 갱신하지 못했어요. 잠시 후 다시 시도해 주세요.')
  }

  const body: ApiResponse<LoginResponse> = await response.json()

  if (
    !body.success ||
    typeof body.data?.accessToken !== 'string' ||
    !body.data.accessToken ||
    body.data.tokenType !== 'Bearer'
  ) {
    throw new Error('토큰 갱신 응답을 확인하지 못했어요.')
  }

  // 갱신 도중 새로 로그인했거나 토큰이 삭제된 경우 덮어쓰지 않습니다.
  if (getAccessToken() !== previousToken) {
    throw new AuthenticationRequiredError()
  }

  const nextToken = body.data.accessToken
  saveAccessToken(nextToken)
  lastRotation = { previous: previousToken, current: nextToken }

  return nextToken
}

export async function authenticatedFetch(
  url: string,
  options: RequestInit = {},
  accessToken?: string,
): Promise<Response> {
  const token = accessToken ?? getAccessToken()

  function send(bearer: string | null): Promise<Response> {
    const headers = new Headers(options.headers)

    if (bearer) {
      headers.set('Authorization', `Bearer ${bearer}`)
    }

    return fetch(url, { ...options, headers })
  }

  const response = await send(token)
  if (response.status !== 401) return response

  // 명시적으로 전달한 토큰이나 비로그인 요청은 자동 갱신하지 않습니다.
  if (accessToken !== undefined || !token) {
    throw new AuthenticationRequiredError()
  }

  let nextToken: string
  const currentToken = getAccessToken()

  if (
    currentToken &&
    lastRotation?.previous === token &&
    lastRotation.current === currentToken
  ) {
    // 늦게 도착한 401은 이미 갱신된 토큰을 사용합니다.
    nextToken = currentToken
  } else {
    if (currentToken !== token) {
      throw new AuthenticationRequiredError()
    }

    // 동시에 여러 요청이 401을 받아도 갱신은 한 번만 합니다.
    refreshInFlight ??= refreshAccessToken(token).finally(() => {
      refreshInFlight = null
    })

    nextToken = await refreshInFlight
  }

  if (options.signal?.aborted) throw options.signal.reason
  if (getAccessToken() !== nextToken) {
    throw new AuthenticationRequiredError()
  }

  const retried = await send(nextToken)
  if (retried.status === 401) {
    throw new AuthenticationRequiredError()
  }

  return retried
}