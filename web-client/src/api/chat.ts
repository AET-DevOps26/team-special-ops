import { authHeaders, fetchJson } from './client'
import type { components } from './types'

export type ChatAnswerResponse = components['schemas']['ChatAnswerResponse']
export type ChatQuestionRequest = components['schemas']['ChatQuestionRequest']

export function askQuestion(
  token: string,
  body: ChatQuestionRequest,
): Promise<ChatAnswerResponse> {
  return fetchJson<ChatAnswerResponse>('/chat/questions', {
    method: 'POST',
    headers: authHeaders(token),
    body: JSON.stringify(body),
  })
}
