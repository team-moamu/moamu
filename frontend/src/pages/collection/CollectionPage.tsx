import { useState } from 'react'
import collectionMu from '../../assets/images/collection_mu.png'
import { SectionTitle } from '../../components/atoms/typography/SectionTitle'
import { CollectionFilter, type CollectionFilterValue } from '../../components/molecules/select/CollectionFilter'

export function CollectionPage() {
  const [filter, setFilter] = useState<CollectionFilterValue>('all')

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
      </section>
    </>
  )
}
