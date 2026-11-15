import collectionMu from '../../assets/images/collection_mu.png'
import { SectionTitle } from '../../components/atoms/typography/SectionTitle'

export function CollectionPage() {
  return (
    <>
      <div className="relative w-full">
        <img src={collectionMu} alt="" className="block w-full" />
      </div>
      <section className="px-6 py-5">
        <SectionTitle>진행된 작품</SectionTitle>
      </section>
    </>
  )
}
