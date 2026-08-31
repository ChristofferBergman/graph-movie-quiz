import { cleanup, render, screen } from '@testing-library/react'
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
    const game = {
      id: 'b9454d2d-23cc-43ea-a043-dfa05cba079a',
      player: 'Chris',
      score: 0,
      remainingRag: 2,
      remainingGraphRag: 2,
      question: {
        movie: 'Rogue One',
        person: 'Robert Duvall',
        ragUsed: false,
        graphRagUsed: false,
      },
    }
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
})
