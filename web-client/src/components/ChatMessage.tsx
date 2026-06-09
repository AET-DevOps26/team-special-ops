import type { Episode } from '../api/catalog'

type ChatMessageProps = {
  role: 'user' | 'assistant'
  text: string
  citedEpisodeIndices?: number[]
  episodes?: Episode[]
}

function formatCitation(episodes: Episode[], index: number): string {
  const ep = episodes.find((e) => e.episodeIndex === index)
  return ep ? `S${ep.season}E${ep.episodeNumber}` : `Ep ${index}`
}

export function ChatMessage({
  role,
  text,
  citedEpisodeIndices = [],
  episodes = [],
}: ChatMessageProps) {
  const isUser = role === 'user'

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[85%] rounded-xl px-4 py-3 text-sm ${
          isUser
            ? 'bg-slate-800 text-white'
            : 'border border-slate-200 bg-white text-slate-800 shadow-sm'
        }`}
      >
        <p className="whitespace-pre-wrap">{text}</p>
        {!isUser && citedEpisodeIndices.length > 0 && (
          <p className="mt-2 text-xs text-slate-500">
            Based on{' '}
            {citedEpisodeIndices
              .map((idx) => formatCitation(episodes, idx))
              .join(', ')}
          </p>
        )}
      </div>
    </div>
  )
}
