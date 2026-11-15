import { useState } from 'react'
import collectionMu from '../../assets/images/collection_mu.png'
import namecardArtwork from '../../assets/images/artworks/namecard.png'
import { CollectionArtworkCard } from '../../components/molecules/card/CollectionArtworkCard'
import { SectionTitle } from '../../components/atoms/typography/SectionTitle'
import { CollectionSearchBar } from '../../components/molecules/form/CollectionSearchBar'
import { CollectionFilter, type CollectionFilterValue } from '../../components/molecules/select/CollectionFilter'

export function CollectionPage() {
  const [filter, setFilter] = useState<CollectionFilterValue>('all')
  const [keyword, setKeyword] = useState('')

  return (
    <>
      <div className="relative w-full">
        <img src={collectionMu} alt="" className="block w-full" />
      </div>
      <section className="px-6 py-5">
        <SectionTitle>진행된 작품</SectionTitle>
        <div className="mt-5">
          <CollectionFilter value={filter} onChange={setFilter} />
        </div>
        <CollectionSearchBar
          className="mt-[3.56%]"
          inputProps={{
            value: keyword,
            onChange: (event) => setKeyword(event.target.value),
          }}
        />
        {/* API 연결 전 레이아웃 확인용 예시 데이터 */}
        <div className="mt-1">
          <CollectionArtworkCard
            challengeTitle="명찰 챙긴 모나리자"
            artwork={{ imageUrl: namecardArtwork, artworkTitle: '모나리자' }}
            progress={{ durationDays: 7, successDays: 5, progressPercent: 71 }}
            isExhibited={true}
          />
        </div>
      </section>
    </>
  )
}
