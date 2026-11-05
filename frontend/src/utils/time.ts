export function getRemainingTime(): string {
  const now = new Date()

  // 다음 날 00:00
  const midnight = new Date(now)
  midnight.setHours(24, 0, 0, 0)

  const diff = midnight.getTime() - now.getTime()

  const hours = Math.floor(diff / (1000 * 60 * 60))
  const minutes = Math.floor((diff / (1000 * 60)) % 60)

  return `${hours}시간 ${minutes}분`
}