import assert from 'node:assert/strict'
import { test } from 'node:test'
import { centeredDates, dateKey, demoChallenges, toHomeChallenge } from './homeData.ts'

const today = new Date(2026, 10, 8, 12)

test('selection stays fourth across month, year and leap-day boundaries', () => {
  for (const key of ['2026-11-08', '2026-11-07', '2026-11-01', '2027-01-01', '2024-03-01']) {
    const dates = centeredDates(new Date(`${key}T12:00:00`))
    assert.equal(dates.length, 7)
    assert.equal(dateKey(dates[3]), key)
    for (let index = 1; index < dates.length; index++) {
      assert.equal(Math.round((dates[index].getTime() - dates[index - 1].getTime()) / 86400000), 1)
    }
  }
  assert.equal(dateKey(centeredDates(new Date(2027, 0, 1, 12))[0]), '2026-12-29')
  assert.equal(dateKey(centeredDates(new Date(2024, 2, 1, 12))[2]), '2024-02-29')
})

test('today and yesterday have their own challenge list and progress', () => {
  const summary = (date) => demoChallenges(date, today).map(c => [c.title, c.day, c.pieces])
  assert.deepEqual(summary(today), [['명찰 챙기기', 4, 3], ['물 마시기', 1, 0]])
  assert.deepEqual(summary(new Date(2026, 10, 7, 12)), [['명찰 챙기기', 3, 2], ['계단 오르기', 1, 0]])
})

test('a date without challenges has an empty list', () => {
  assert.deepEqual(demoChallenges(new Date(2026, 10, 1, 12), today), [])
})

test('API mapping counts successful pieces only through the selected date', () => {
  const challenge = {
    challengeId: 5,
    scheduleId: 37,
    challengeTitle: '명찰 착용 기록하기',
    startDate: '2026-10-31',
    endDate: '2026-11-13',
    dayNo: 9,
    scheduleStatus: 'OPEN',
    attemptCount: 0,
    resultReason: null,
  }
  const history = [
    { scheduleDate: '2026-11-06', scheduleStatus: 'SUCCESS' },
    { scheduleDate: '2026-11-07', scheduleStatus: 'FAILED' },
    { scheduleDate: '2026-11-08', scheduleStatus: 'SUCCESS' },
    { scheduleDate: '2026-11-09', scheduleStatus: 'SUCCESS' },
  ]

  const yesterday = toHomeChallenge(challenge, history, '2026-11-07')
  const today = toHomeChallenge(challenge, history, '2026-11-08')

  assert.equal(yesterday.pieces, 1)
  assert.equal(today.pieces, 2)
  assert.equal(today.duration, 14)
  assert.equal(today.status, 'OPEN')
  assert.equal(today.scheduleId, 37)
})

test('missing history remains unknown instead of showing zero pieces', () => {
  const challenge = {
    challengeId: 1,
    scheduleId: 1,
    challengeTitle: '물 마시기',
    startDate: '2026-11-07',
    endDate: '2026-11-13',
    dayNo: 2,
    scheduleStatus: 'UPCOMING',
  }

  assert.equal(toHomeChallenge(challenge, null, '2026-11-08').pieces, null)
  assert.equal(toHomeChallenge(challenge, [], '2026-11-08').pieces, 0)
})

