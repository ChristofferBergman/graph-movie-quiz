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
    expect(screen.getByLabelText('Powered by Neo4j')).toBeInTheDocument()
  })

  it('opens and closes the instructions without closing on a scroll gesture', async () => {
    const user = userEvent.setup()
    render(<App />)

    const instructionsButton = screen.getByRole('button', { name: 'Instructions' })
    await user.click(instructionsButton)
    const dialog = screen.getByRole('dialog', { name: 'Instructions' })
    expect(dialog).toHaveFocus()

    fireEvent.pointerDown(dialog, { clientX: 10, clientY: 10 })
    fireEvent.pointerMove(dialog, { clientX: 10, clientY: 40 })
    fireEvent.pointerUp(dialog, { clientX: 10, clientY: 40 })
    expect(dialog).toBeInTheDocument()

    const instructionsPanel = dialog.querySelector('article')
    expect(instructionsPanel).not.toBeNull()
    fireEvent.pointerDown(dialog, { clientX: 10, clientY: 10 })
    fireEvent.scroll(instructionsPanel!)
    fireEvent.pointerUp(dialog, { clientX: 10, clientY: 10 })
    expect(dialog).toBeInTheDocument()

    fireEvent.pointerDown(dialog, { clientX: 10, clientY: 10 })
    fireEvent.pointerUp(dialog, { clientX: 10, clientY: 10 })
    expect(screen.queryByRole('dialog', { name: 'Instructions' })).not.toBeInTheDocument()
    expect(instructionsButton).toHaveFocus()
  })

  it('starts a game and shows the first question', async () => {
    const game = createTestGame()
    vi.stubGlobal(
      'fetch',
      vi.fn()
        .mockResolvedValueOnce(jsonResponse(game, { status: 201 }))
        .mockResolvedValueOnce(jsonResponse([])),
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
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(game))
      .mockResolvedValueOnce(jsonResponse([
        { player: 'Jane', score: 6 },
        { player: 'John', score: 5 },
      ]))
    vi.stubGlobal('fetch', fetchMock)

    render(<App />)

    expect(screen.getByRole('status')).toHaveTextContent('Loading game')
    expect(await screen.findByText('Score (completed questions): 3')).toBeInTheDocument()
    expect(screen.getByLabelText('High scores')).toHaveTextContent('1Jane62John5')
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('ends the game when the question timer expires', async () => {
    const game = {
      ...createTestGame({ score: 2 }),
      questionDeadline: new Date(Date.now() - 1000).toISOString(),
    }
    localStorage.setItem('graphrag-movie-quiz.game-id', game.id)
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(game))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse({
        correct: false,
        score: 2,
        correctAnswer: 'Diego Luna',
        game: null,
      }))
      .mockResolvedValueOnce(jsonResponse([]))
    vi.stubGlobal('fetch', fetchMock)
    render(<App />)

    expect(await screen.findByRole('heading', { name: 'Game over' })).toBeInTheDocument()
    expect(screen.getByText('The correct answer was Diego Luna.')).toBeInTheDocument()
    expect(localStorage.getItem('graphrag-movie-quiz.game-id')).toBeNull()
    expect(fetchMock).toHaveBeenCalledWith(
      `http://localhost:8080/api/v1/games/${game.id}/timeout`,
      expect.objectContaining({ method: 'POST' }),
    )
  })

  it('closes the active game and clears it from the browser', async () => {
    const game = createTestGame()
    localStorage.setItem('graphrag-movie-quiz.game-id', game.id)
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(game))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(new Response(null, { status: 204 }))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    await user.click(await screen.findByRole('button', { name: 'Close game' }))

    expect(await screen.findByLabelText('Player name')).toBeInTheDocument()
    expect(localStorage.getItem('graphrag-movie-quiz.game-id')).toBeNull()
    expect(fetchMock).toHaveBeenLastCalledWith(
      `http://localhost:8080/api/v1/games/${game.id}`,
      expect.objectContaining({ method: 'DELETE' }),
    )
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
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(
        jsonResponse({
          correct: false,
          score: 2,
          correctAnswer: 'Diego Luna',
          game: null,
        }),
      )
      .mockResolvedValueOnce(jsonResponse([{ player: 'Chris', score: 2 }]))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    await user.type(await screen.findByLabelText('Your answer'), 'Wrong Actor')
    await user.click(screen.getByRole('button', { name: 'Submit answer' }))

    expect(await screen.findByRole('heading', { name: 'Game over' })).toBeInTheDocument()
    expect(screen.getByText('Final score: 2')).toBeInTheDocument()
    expect(screen.getByText('The correct answer was Diego Luna.')).toBeInTheDocument()
    expect(screen.getByLabelText('High scores')).toHaveTextContent('1Chris2')
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
      .mockResolvedValueOnce(jsonResponse([]))
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
        name: 'Use RAG token 1',
      }),
    )
    const clueCard = await screen.findByRole('button', {
      name: 'Rogue One clue',
    })
    await user.click(clueCard)

    expect(await screen.findByText(/Felicity Jones/)).toHaveTextContent(
      'Felicity Jones Diego Luna',
    )

    fireEvent.pointerDown(clueCard, {
      pointerType: 'touch',
      clientX: 10,
      clientY: 10,
    })
    await new Promise((resolve) => window.setTimeout(resolve, 550))
    const touchZoomedClue = screen.getByRole('dialog', { name: 'Rogue One clue' })
    expect(
      touchZoomedClue.querySelector(
        '.clue-card--zoomed > img.clue-card__image',
      ),
    ).not.toBeNull()
    expect(touchZoomedClue.querySelector('.clue-card--zoomed > .clue-details')).not.toBeNull()
    expect(touchZoomedClue.querySelector('.clue-card--zoomed .clue-card__inner')).toBeNull()
    fireEvent.pointerDown(touchZoomedClue)
    expect(screen.queryByRole('dialog', { name: 'Rogue One clue' })).not.toBeInTheDocument()

    fireEvent.contextMenu(clueCard)
    const zoomedClue = screen.getByRole('dialog', { name: 'Rogue One clue' })
    expect(zoomedClue).toBeInTheDocument()
    expect(
      screen.getByText('Right-click a revealed card or focus it and press Z to zoom'),
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument()
    fireEvent.pointerDown(zoomedClue, { button: 2 })
    expect(screen.queryByRole('dialog', { name: 'Rogue One clue' })).not.toBeInTheDocument()

    fireEvent.keyDown(clueCard, { key: 'z' })
    const keyboardZoomedClue = screen.getByRole('dialog', { name: 'Rogue One clue' })
    expect(keyboardZoomedClue).toHaveFocus()
    fireEvent.keyDown(keyboardZoomedClue, { key: 'Escape' })
    expect(screen.queryByRole('dialog', { name: 'Rogue One clue' })).not.toBeInTheDocument()
    expect(clueCard).toHaveFocus()
    expect(fetchMock).toHaveBeenCalledTimes(4)
  })

  it('shows the connection movie name immediately after using GraphRAG', async () => {
    const game = createTestGame()
    const unlockedGame = {
      ...game,
      remainingGraphRag: 1,
      question: {
        ...game.question,
        connectionMovie: 'Open Range',
        graphRagUsed: true,
      },
    }
    localStorage.setItem('graphrag-movie-quiz.game-id', game.id)
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(game))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse(unlockedGame))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    await user.click(
      await screen.findByRole('button', { name: 'Use GraphRAG token 1' }),
    )

    expect(
      await screen.findByRole('button', { name: 'Open Range clue' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'GraphRAG token 1, used' })).toBeDisabled()
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('does not reopen suggestions after selecting an autocomplete result', async () => {
    const game = createTestGame()
    localStorage.setItem('graphrag-movie-quiz.game-id', game.id)
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(game))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(jsonResponse([{ name: 'Diego Luna' }]))
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    const answer = await screen.findByLabelText('Your answer')
    await user.type(answer, 'Di')
    await user.click(await screen.findByRole('option', { name: 'Diego Luna' }))

    expect(answer).toHaveValue('Diego Luna')
    expect(screen.queryByLabelText('Actor suggestions')).not.toBeInTheDocument()
    await new Promise((resolve) => window.setTimeout(resolve, 250))
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })

  it('selects an autocomplete result with the keyboard', async () => {
    const game = createTestGame()
    localStorage.setItem('graphrag-movie-quiz.game-id', game.id)
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse(game))
      .mockResolvedValueOnce(jsonResponse([]))
      .mockResolvedValueOnce(
        jsonResponse([{ name: 'Diana Lee Inosanto' }, { name: 'Diego Luna' }]),
      )
    vi.stubGlobal('fetch', fetchMock)
    const user = userEvent.setup()
    render(<App />)

    const answer = await screen.findByLabelText('Your answer')
    await user.type(answer, 'Di')
    const options = await screen.findAllByRole('option')

    await user.keyboard('{ArrowDown}{ArrowDown}')
    expect(options[1]).toHaveAttribute('aria-selected', 'true')
    expect(answer).toHaveAttribute('aria-activedescendant', 'actor-suggestion-1')

    await user.keyboard('{Enter}')
    expect(answer).toHaveValue('Diego Luna')
    expect(screen.queryByLabelText('Actor suggestions')).not.toBeInTheDocument()
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
    questionDeadline: new Date(Date.now() + 40_000).toISOString(),
    question: {
      movie: 'Rogue One',
      person: 'Robert Duvall',
      connectionMovie: null,
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
