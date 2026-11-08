import artworkUrl from './artwork-preview.svg'
import type { ChallengeVerificationResponse, VerificationSuccessResponse } from '../types/Challenge'

// 실제 사용자 데이터와 무관한 로컬 미리보기 전용 샘플
export const sampleChallenge: ChallengeVerificationResponse = {
  challengeId: 5,
  challengeTitle: '명찰 챙기기',
  scheduleId: 36,
  dayNo: 3,
  dayCount: 14,
  attemptCount: 1,
  description: ['명찰이 보여요', '명찰을 들고 있는 손이 보여요'],
}

export function sampleSuccess(verificationId = 101): VerificationSuccessResponse {
  return {
    verificationId,
    status: 'SUCCESS',
    challengeId: 5,
    challengeTitle: '명찰 챙기기',
    scheduleId: 36,
    dayNo: 3,
    scheduleStatus: 'SUCCESS',
    artwork: { artworkId: 20, title: '연못과 다리 · 미리보기 샘플', imageUrl: artworkUrl },
    gridRows: 3,
    gridCols: 5,
    openedPieceIndex: 10,
    revealedPieceIndices: [0, 1, 10],
    earnedPieceCount: 3,
    totalPieceCount: 14,
    checks: [
      { conditionId: 1, code: 'C1', description: '명찰이 보여요', status: 'MET', reason: '명찰이 선명하게 보여요.' },
      { conditionId: 2, code: 'C2', description: '명찰을 들고 있는 손이 보여요', status: 'MET', reason: '명찰을 들고 있는 손이 확인돼요.' },
    ],
  }
}

