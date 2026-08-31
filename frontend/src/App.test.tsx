import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

describe('App', () => {
  afterEach(() => {
    cleanup()
    vi.unstubAllGlobals()
    localStorage.clear()
  })

  it('renders the game title', () => {
    render(<App />)

    expect(
      screen.getByRole('heading', { name: 'GraphRAG Online Movie Quiz' }),
    ).toBeInTheDocument()
  })

  it('starts a game and shows the first question', async () => {
    const game = createTestGame()
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        new Response(JSON.stringify(game), {
          status: 201,
          headers: { 'Content-Type': 'application/json' },
        }),
      ),
    )
    const user = userEvent.setup()
    render(<App />)

    await user.type(screen.getByLabelText('Player name'), 'Chris')
    await user.click(screen.getByRole('button', { name: 'Start game' }))

    expect(
      await screen.findByRole('heading', {
        name: 'Who in Rogue One starred in another movie with Robert Duvall?',
      }),
    ).toBeInTheDocument()
    expect(localStorage.getItem('graphrag-movie-quiz.game-id')).toBe(game.id)
  })

  it('restores a saved game after a browser refresh', async () => {
    const game = createTestGame({ score: 3 })
    localStorage.setItem('graphrag-movie-quiz.game-id', game.id)
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse(game))
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)

    expect(screen.getByRole('status')).toHaveTextContent('Loading game')
    expect(await screen.findByText('Score (completed questions): 3')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledOnce()
  })

  it('clears an expired saved game and allows a new game to start', async () => {
    localStorage.setItem('graphrag-movie-quiz.game-id', 'expired-id')
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue(
        jsonResponse(
          { detail: 'Game not found.' },
          { status: 404 },
        ),
      ),
    )

    render(<App />)

    expect(await screen.findByLabelText('Player name')).toBeInTheDocument()
    expect(screen.getByText(/previous game is invalid or has expired/i)).toBeInTheDocument()
    expect(localStorage.getItem('graphrag-movie-quiz.game-id')).toBeNull()
  })

  it('shows game over and supports restarting after an incorrect answer', async () => {
    const game = createTestGame({ score: 2 })
    localStorage.setItem('graphrag-movie-quiz.game-id', game.id)
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(game))
      .mockResolvedValueOnce(
        jsonResponse({
          correct: false,
          score: 2,
          correctAnswer: 'Diego Luna',
          game: null,
        }),
      )
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    await user.type(await screen.findByLabelText('Your answer'), 'Wrong Actor')
    await user.click(screen.getByRole('button', { name: 'Submit answer' }))

    expect(await screen.findByRole('heading', { name: 'Game over' })).toBeInTheDocument()
    expect(screen.getByText('Final score: 2')).toBeInTheDocument()
    expect(screen.getByText('The correct answer was Diego Luna.')).toBeInTheDocument()
    expect(localStorage.getItem('graphrag-movie-quiz.game-id')).toBeNull()

    await user.click(screen.getByRole('button', { name: 'Play again' }))
    expect(screen.getByLabelText('Player name')).toBeInTheDocument()
  })

  it('uses a RAG token, unlocks the question clue, flips it, and zooms it', async () => {
    const game = createTestGame()
    const unlockedGame = {
      ...game,
      remainingRag: 1,
      question: { ...game.question, ragUsed: true },
    }
    localStorage.setItem('graphrag-movie-quiz.game-id', game.id)
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(game))
      .mockResolvedValueOnce(jsonResponse(unlockedGame))
      .mockResolvedValueOnce(
        jsonResponse({
          movie: 'Rogue One',
          actors: ['Felicity Jones', 'Diego Luna'],
        }),
      )
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      await screen.findByRole('button', {
        name: 'Use RAG token, 2 remaining',
      }),
    )
    const clueCard = await screen.findByRole('button', {
      name: 'Rogue One clue',
    })
    await user.click(clueCard)

    expect(await screen.findByText(/Felicity Jones/)).toHaveTextContent(
      'Felicity Jones Diego Luna',
    )
    fireEvent.contextMenu(clueCard)
    expect(screen.getByRole('dialog', { name: 'Rogue One clue' })).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })
})

function createTestGame(overrides: { score?: number } = {}) {
  return {
    id: 'b9454d2d-23cc-43ea-a043-dfa05cba079a',
    player: 'Chris',
    score: overrides.score ?? 0,
    remainingRag: 2,
    remainingGraphRag: 2,
    question: {
      movie: 'Rogue One',
      person: 'Robert Duvall',
      ragUsed: false,
      graphRagUsed: false,
    },
  }
}

function jsonResponse(body: unknown, init: ResponseInit = {}) {
  return new Response(JSON.stringify(body), {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init.headers },
  })
}
