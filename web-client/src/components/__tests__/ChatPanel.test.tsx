import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ChatPanel } from '../ChatPanel'

vi.mock('../../api/chat', () => ({ askQuestion: vi.fn() }))

import { askQuestion } from '../../api/chat'
import { ApiRequestError } from '../../api/client'

const mockAsk = vi.mocked(askQuestion)

const episodes = [
  { id: 'e1', season: 1, episodeNumber: 1, episodeIndex: 1, title: 'One', summary: 's1' },
  { id: 'e2', season: 1, episodeNumber: 2, episodeIndex: 2, title: 'Two', summary: 's2' },
]

afterEach(() => vi.clearAllMocks())

describe('ChatPanel', () => {
  it('disables send when progress is zero', () => {
    render(
      <ChatPanel token="tok" seriesId="st-1" progress={0} episodes={episodes} />,
    )

    expect(screen.getByRole('button', { name: /send/i })).toBeDisabled()
    expect(screen.getByPlaceholderText(/set your current episode/i)).toBeDisabled()
  })

  it('submits a question and shows the answer with citations', async () => {
    mockAsk.mockResolvedValue({
      id: 'ans-1',
      question: 'Who is Eleven?',
      answer: 'Eleven is a girl with powers.',
      citedEpisodeIndices: [1, 2],
      progressAtAsk: 2,
      createdAt: '2026-01-01T00:00:00Z',
    })

    render(
      <ChatPanel token="tok" seriesId="st-1" progress={2} episodes={episodes} />,
    )

    await userEvent.type(screen.getByRole('textbox'), 'Who is Eleven?')
    await userEvent.click(screen.getByRole('button', { name: /send/i }))

    expect(mockAsk).toHaveBeenCalledWith('tok', {
      seriesId: 'st-1',
      question: 'Who is Eleven?',
    })

    expect(await screen.findByText('Eleven is a girl with powers.')).toBeInTheDocument()
    expect(screen.getByText(/Based on S1E1, S1E2/)).toBeInTheDocument()
  })

  it('shows an error when the API returns 502', async () => {
    mockAsk.mockRejectedValue(new ApiRequestError(502, 'GenAI down', 'GENAI_UNAVAILABLE'))

    render(
      <ChatPanel token="tok" seriesId="st-1" progress={2} episodes={episodes} />,
    )

    await userEvent.type(screen.getByRole('textbox'), 'Who is Eleven?')
    await userEvent.click(screen.getByRole('button', { name: /send/i }))

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/temporarily unavailable/i)
    })
  })
})
