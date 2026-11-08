import { clearAccessToken, getAccessToken } from '../auth/authStorage.ts'

export class AuthenticationRequiredError extends Error {
  constructor() {
    super('다시 로그인해 주세요.')
    this.name = 'AuthenticationRequiredError'
  }
}

// 직접 전달한 토큰이 없으면 로그인 페이지에서 저장한 세션 토큰을 사용한다.
export async function authenticatedFetch(
  url: string,
  options: RequestInit = {},
  accessToken?: string,
): Promise<Response> {
  const token = accessToken ?? getAccessToken()
  const headers = new Headers(options.headers)

  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  const response = await fetch(url, { ...options, headers })

  if (response.status === 401) {
    // 이전 요청의 늦은 실패가 새 로그인 토큰을 지우지 않도록 한다.
    if (token && getAccessToken() === token) {
      clearAccessToken()
    }
    throw new AuthenticationRequiredError()
  }

  return response
}
