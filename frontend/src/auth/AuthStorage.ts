const ACCESS_TOKEN_KEY = 'moamu.accessToken'

export function saveAccessToken(token: string): void {
  sessionStorage.setItem(ACCESS_TOKEN_KEY, token)
}

export function getAccessToken(): string | null {
  return sessionStorage.getItem(ACCESS_TOKEN_KEY)
}

export function clearAccessToken(): void {
  sessionStorage.removeItem(ACCESS_TOKEN_KEY)
  sessionStorage.removeItem(LOGIN_EMAIL_KEY)
}

const LOGIN_EMAIL_KEY = 'moamu.loginEmail'

export function saveLoginEmail(email: string): void {
  sessionStorage.setItem(LOGIN_EMAIL_KEY, email)
}

export function getLoginEmail(): string | null {
  return getAccessToken() ? sessionStorage.getItem(LOGIN_EMAIL_KEY) : null
}