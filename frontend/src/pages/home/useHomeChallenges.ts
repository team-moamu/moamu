import { useEffect, useState } from 'react'
import { getChallengeDetail, getTodayChallenges } from '../../api/ChallengeApi'
import { AuthenticationRequiredError } from '../../api/authenticatedFetch'
import { getAccessToken } from '../../auth/authStorage'
import { centeredDates, dateKey, toHomeChallenge } from './homeData'
import type { HomeDataMode, HomeRecords } from './homeData'

interface HomeDataState {
  selectedKey: string
  mode: HomeDataMode
  records: HomeRecords
}

async function loadHomeRecords(selectedDate: Date, signal: AbortSignal): Promise<HomeRecords> {
  const dates = centeredDates(selectedDate)
  const dailyChallenges = await Promise.all(
    dates.map(date => getTodayChallenges(dateKey(date), signal)),
  )

  // 여러 날짜에 같은 도전이 있으므로 상세 기록은 도전별로 한 번만 조회한다.
  const challengeIds = [...new Set(dailyChallenges.flat().map(challenge => challenge.challengeId))]
  const histories = new Map(await Promise.all(
    challengeIds.map(async id => (
      [id, await getChallengeDetail(id, signal).catch(error => {
        if (error instanceof AuthenticationRequiredError) throw error
        return null
      })] as const
    )),
  ))

  return Object.fromEntries(dates.map((date, index) => {
    const key = dateKey(date)
    const challenges = dailyChallenges[index].map(challenge => (
      toHomeChallenge(challenge, histories.get(challenge.challengeId) ?? null, key)
    ))

    return [key, challenges]
  }))
}

export function useHomeChallenges(selectedDate: Date) {
  const accessToken = getAccessToken()
  const selectedKey = dateKey(selectedDate)
  const [state, setState] = useState<HomeDataState>({
    selectedKey,
    mode: 'loading',
    records: {},
  })

  useEffect(() => {
    if (!accessToken) return

    const controller = new AbortController()

    async function load() {
      try {
        const records = await loadHomeRecords(selectedDate, controller.signal)

        if (!controller.signal.aborted) {
          setState({ selectedKey: dateKey(selectedDate), mode: 'live', records })
        }
      } catch (error) {
        if (!controller.signal.aborted) {
          const mode = error instanceof AuthenticationRequiredError ? 'unauthenticated' : 'error'
          setState({ selectedKey: dateKey(selectedDate), mode, records: {} })
        }
      }
    }

    void load()
    return () => controller.abort()
  }, [selectedDate, accessToken])

  // 날짜 변경 직후에는 이전 날짜의 도전이 보이지 않도록 한다.
  let mode: HomeDataMode = 'loading'
  if (!accessToken) {
    mode = 'unauthenticated'
  } else if (state.selectedKey === selectedKey) {
    mode = state.mode
  }
  const records = state.selectedKey === selectedKey ? state.records : {}
  const challenges = records[selectedKey] ?? []

  const dates = centeredDates(selectedDate).map(date => ({
    date,
    hasRecord: (records[dateKey(date)]?.length ?? 0) > 0,
  }))

  return { mode, challenges, dates }
}
