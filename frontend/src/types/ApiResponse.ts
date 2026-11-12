export type ApiResponse<T> =
  | { success: true; data: T; error?: never }
  | { success: false; data?: never; error: { code: string; message: string } }
