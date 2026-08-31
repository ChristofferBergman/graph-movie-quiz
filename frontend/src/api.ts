export interface Question {
  movie: string
  person: string
  ragUsed: boolean
  graphRagUsed: boolean
}

export interface Game {
  id: string
  player: string
  score: number
  remainingRag: number
  remainingGraphRag: number
  question: Question
}

export interface ActorSuggestion {
  name: string
}

export interface SubmitAnswerResponse {
  correct: boolean
  score: number
  game: Game | null
}

interface ProblemDetails {
  detail?: string
  title?: string
}

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const problem = (await response.json().catch(() => ({}))) as ProblemDetails
    throw new ApiError(
      problem.detail ?? problem.title ?? 'The request failed.',
      response.status,
    )
  }

  return response.json() as Promise<T>
}

export function createGame(player: string): Promise<Game> {
  return request<Game>('/api/v1/games', {
    method: 'POST',
    body: JSON.stringify({ player }),
  })
}

export function loadGame(gameId: string): Promise<Game> {
  return request<Game>(`/api/v1/games/${gameId}`)
}

export function findActorSuggestions(
  prefix: string,
  signal?: AbortSignal,
): Promise<ActorSuggestion[]> {
  const query = new URLSearchParams({ prefix })
  return request<ActorSuggestion[]>(`/api/v1/actors/suggestions?${query}`, {
    signal,
  })
}

export function submitAnswer(
  gameId: string,
  name: string,
): Promise<SubmitAnswerResponse> {
  return request<SubmitAnswerResponse>(`/api/v1/games/${gameId}/answers`, {
    method: 'POST',
    body: JSON.stringify({ name }),
  })
}
