// 작품 이미지는 별도 레이어로 넣을 수 있는 나무 이젤 장식이다.
export function CanvasEasel() {
  return (
    <svg viewBox="0 0 240 340" fill="none" aria-hidden="true" className="gallery-easel-decoration">
      <ellipse cx="120" cy="328" rx="104" ry="10" fill="#46301c" opacity=".2" />
      <g fill="#b67b38" stroke="#70451f" strokeWidth="3" strokeLinejoin="round">
        <path d="M112 10 Q120 2 128 10 L128 305 L112 305Z" />
        <path d="M60 180 L78 183 L52 328 L34 328Z" />
        <path d="M162 183 L180 180 L206 328 L188 328Z" />
        <path d="M65 270 L175 270 L178 284 L62 284Z" />
        <rect x="20" y="221" width="200" height="16" rx="3" />
      </g>
      <path d="M118 20V46 M44 316L57 251 M196 316L183 251 M72 275H168" stroke="#edbe72" strokeWidth="3" />
      <rect x="23" y="47" width="194" height="184" rx="3" fill="#9a652b" stroke="#643b19" strokeWidth="3" />
      <rect x="28" y="52" width="184" height="174" rx="2" stroke="#f4d38c" strokeWidth="5" />
      <g fill="#f4d38c" stroke="#a67129" strokeWidth="1">
        <path d="M29 53H50L29 74Z M211 53H190L211 74Z M29 225H50L29 204Z M211 225H190L211 204Z" />
        <circle cx="120" cy="228" r="5" />
      </g>
      <path d="M192 24C219 65 206 102 225 135S213 194 226 249" stroke="#547334" strokeWidth="4" />
      <g fill="#6f9144" stroke="#45652b" strokeWidth="1">
        <ellipse cx="199" cy="43" rx="6" ry="13" transform="rotate(-35 199 43)" />
        <ellipse cx="213" cy="65" rx="6" ry="13" transform="rotate(40 213 65)" />
        <ellipse cx="210" cy="104" rx="6" ry="13" transform="rotate(-35 210 104)" />
        <ellipse cx="224" cy="144" rx="6" ry="13" transform="rotate(35 224 144)" />
        <ellipse cx="218" cy="189" rx="6" ry="13" transform="rotate(-35 218 189)" />
        <ellipse cx="227" cy="228" rx="6" ry="13" transform="rotate(35 227 228)" />
      </g>
      {[{x: 204, y: 50}, {x: 220, y: 161}, {x: 30, y: 220}].map(({x, y}) => (
        <g key={y} transform={`translate(${x} ${y})`}>
          {[0, 72, 144, 216, 288].map(angle => <ellipse key={angle} cy="-6" rx="3" ry="5" fill="#fff9df" transform={`rotate(${angle})`} />)}
          <circle r="3" fill="#e4b64f" />
        </g>
      ))}
    </svg>
  )
}
