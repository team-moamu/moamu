import { marketingSections, privacyRows, serviceArticles, signupTerms } from './signupTerms'

function renderEmphasis(text: string) {
  return text.split('**').map((part, index) =>
    index % 2 === 0 ? part : <strong key={index}>{part}</strong>,
  )
}

export function SignupTermsContent({ title }: { title: (typeof signupTerms)[number]['title'] }) {
  switch (title) {
    case '서비스 이용약관':
      return (
        <>
          <h3 className="text-body font-bold">모아뮤(MOAMU) 서비스 이용약관</h3>
          {serviceArticles.map(([heading, clauses]) => (
            <section key={heading} className="space-y-2">
              <h4 className="font-semibold">{heading}</h4>
              <ol className="list-decimal space-y-2 pl-5">
                {clauses.map((clause) => <li key={clause}>{renderEmphasis(clause)}</li>)}
              </ol>
            </section>
          ))}
          <section className="space-y-2">
            <h4 className="font-semibold">제8조 운영자 및 문의처</h4>
            <p>회원은 서비스 이용, 사진 인증 결과, 이용 제한 또는 탈퇴에 관해 아래 문의처로 문의할 수 있습니다.</p>
            <ul className="list-disc pl-5">
              <li><strong>운영 주체:</strong> 팀 모아뮤</li>
              <li><strong>문의처:</strong> moamyu92@gmail.com</li>
            </ul>
          </section>
          <section className="space-y-2">
            <h4 className="font-semibold">부칙</h4>
            <p>이 약관은 2026년 11월 25일부터 시행합니다.</p>
          </section>
        </>
      )
    case '개인정보 수집·이용':
      return (
        <>
          <p>모아뮤는 회원가입과 계정 관리를 위해 아래와 같이 개인정보를 수집·이용합니다.</p>
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="bg-[#FDF7EB]">
                {['수집·이용 항목', '수집·이용 목적', '보유·이용 기간'].map((heading) => (
                  <th key={heading} scope="col" className="border border-[#EBD2B5] p-2">{heading}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {privacyRows.map((row) => (
                <tr key={row[0]}>
                  {row.map((cell) => (
                    <td key={cell} className="border border-[#EBD2B5] p-2 align-top wrap-anywhere">{cell}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
          <p><strong>동의를 거부할 권리가 있습니다.</strong> 다만 위 항목은 이메일 인증과 계정 생성에 필요하므로, 동의하지 않으면 회원가입이 제한됩니다.</p>
          <p>☐ <strong>[필수] 개인정보 수집·이용에 동의합니다.</strong></p>
        </>
      )
    case '마케팅 정보 수신':
      return (
        <>
          {marketingSections.map(([heading, paragraphs]) => (
            <section key={heading} className="space-y-2">
              <h3 className="font-semibold">{heading}</h3>
              {paragraphs.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
            </section>
          ))}
          <p>발송 주체: 팀 모아뮤</p>
          <p>문의: moamyu92@gmail.com</p>
        </>
      )
  }
}
