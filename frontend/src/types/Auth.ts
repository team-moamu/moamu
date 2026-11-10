export interface LoginRequest {
  loginId: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
}

export interface SignupRequest {
  loginId: string
  password: string
}

export interface SignupResponse {
  userId: number
}

export interface EmailAvailabilityResponse {
  available: boolean
}