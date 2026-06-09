import { useState } from 'react'
import { askQuestion } from '../api/chat'
import type { Episode } from '../api/catalog'
import { ApiRequestError } from '../api/client'
import { ChatMessage } from './ChatMessage'

type Message = {
  id: string
  role: 'user' | 'assistant'
  text: string
  citedEpisodeIndices?: number[]
}

type ChatPanelProps = {
  token: string
  seriesId: string
  progress: number
  episodes: Episode[]
}

export function ChatPanel({ token, seriesId, progress, episodes }: ChatPanelProps) {
  const [messages, setMessages] = useState<Message[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const canAsk = progress > 0 && !loading

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const question = input.trim()
    if (!question || !canAsk) return

    const userMsg: Message = { id: crypto.randomUUID(), role: 'user', text: question }
    setMessages((prev) => [...prev, userMsg])
    setInput('')
    setLoading(true)
    setError(null)

    try {
      const answer = await askQuestion(token, { seriesId, question })
      setMessages((prev) => [
        ...prev,
        {
          id: answer.id,
          role: 'assistant',
          text: answer.answer,
          citedEpisodeIndices: answer.citedEpisodeIndices,
        },
      ])
    } catch (err) {
      const message =
        err instanceof ApiRequestError
          ? err.status === 502
            ? 'The AI service is temporarily unavailable. Please try again.'
            : err.message
          : 'Something went wrong. Please try again.'
      setError(message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <aside className="flex h-full min-h-[24rem] flex-col rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 px-4 py-3">
        <h2 className="font-semibold text-slate-900">Ask a question</h2>
        <p className="text-xs text-slate-500">
          Answers use only episodes up to your current progress.
        </p>
      </div>

      <div className="flex-1 space-y-3 overflow-y-auto px-4 py-3">
        {messages.length === 0 && (
          <p className="text-sm text-slate-400">
            {progress > 0
              ? 'Ask about characters, plot, or events you have already seen.'
              : 'Set your current episode first to unlock spoiler-safe answers.'}
          </p>
        )}
        {messages.map((msg) => (
          <ChatMessage
            key={msg.id}
            role={msg.role}
            text={msg.text}
            citedEpisodeIndices={msg.citedEpisodeIndices}
            episodes={episodes}
          />
        ))}
        {loading && (
          <p className="text-sm text-slate-400" role="status">
            Thinking…
          </p>
        )}
      </div>

      {error && (
        <p className="px-4 text-sm text-rose-600" role="alert">
          {error}
        </p>
      )}

      <form onSubmit={handleSubmit} className="border-t border-slate-100 p-4">
        <textarea
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder={
            progress > 0 ? 'Who is Eleven?' : 'Set your current episode first…'
          }
          disabled={!canAsk}
          rows={2}
          className="w-full resize-none rounded-lg border border-slate-200 px-3 py-2 text-sm focus:border-slate-400 focus:outline-none disabled:bg-slate-50 disabled:text-slate-400"
        />
        <button
          type="submit"
          disabled={!canAsk || !input.trim()}
          className="mt-2 w-full rounded-lg bg-slate-800 px-4 py-2 text-sm font-medium text-white hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          Send
        </button>
      </form>
    </aside>
  )
}
