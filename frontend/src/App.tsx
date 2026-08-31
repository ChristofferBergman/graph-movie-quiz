import { useEffect, useRef, useState, type FormEvent } from 'react'
import {
  Banner,
  FilledButton,
  TextButton,
  TextInput,
  Typography,
} from '@neo4j-ndl/react'
import {
  createGame,
  findActorSuggestions,
  loadGame,
  submitAnswer,
  ApiError,
  type ActorSuggestion,
  type Game,
} from './api'
import './App.css'

const GAME_ID_STORAGE_KEY = 'graphrag-movie-quiz.game-id'

function App() {
  const [game, setGame] = useState<Game | null>(null)
  const [finalScore, setFinalScore] = useState<number | null>(null)
  const [correctAnswer, setCorrectAnswer] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(() =>
    Boolean(localStorage.getItem(GAME_ID_STORAGE_KEY)),
  )
  const requestInFlight = useRef(false)

  useEffect(() => {
    const storedGameId = localStorage.getItem(GAME_ID_STORAGE_KEY)
    if (!storedGameId) return

    let isActive = true
    void loadGame(storedGameId)
      .then((loadedGame) => {
        if (isActive) setGame(loadedGame)
      })
      .catch((requestError: unknown) => {
        if (!isActive) return
        if (
          requestError instanceof ApiError &&
          (requestError.status === 400 || requestError.status === 404)
        ) {
          localStorage.removeItem(GAME_ID_STORAGE_KEY)
          setError(
            'Your previous game is invalid or has expired. Start a new game to continue.',
          )
        } else {
          setError(getErrorMessage(requestError))
        }
      })
      .finally(() => {
        if (isActive) setIsLoading(false)
      })

    return () => {
      isActive = false
    }
  }, [])

  async function handleStart(player: string) {
    if (requestInFlight.current) return
    requestInFlight.current = true
    setError(null)
    setIsLoading(true)

    try {
      const createdGame = await createGame(player)
      localStorage.setItem(GAME_ID_STORAGE_KEY, createdGame.id)
      setGame(createdGame)
      setFinalScore(null)
      setCorrectAnswer(null)
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      requestInFlight.current = false
      setIsLoading(false)
    }
  }

  async function handleAnswer(name: string) {
    if (!game || requestInFlight.current) return
    requestInFlight.current = true

    setError(null)
    setIsLoading(true)

    try {
      const result = await submitAnswer(game.id, name)
      if (result.correct && result.game) {
        setGame(result.game)
      } else {
        localStorage.removeItem(GAME_ID_STORAGE_KEY)
        setFinalScore(result.score)
        setCorrectAnswer(result.correctAnswer)
        setGame(null)
      }
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 404) {
        localStorage.removeItem(GAME_ID_STORAGE_KEY)
        setGame(null)
        setError('This game has expired. Start a new game to continue.')
      } else {
        setError(getErrorMessage(requestError))
      }
    } finally {
      requestInFlight.current = false
      setIsLoading(false)
    }
  }

  function handleRestart() {
    setFinalScore(null)
    setCorrectAnswer(null)
    setError(null)
  }

  return (
    <main className="app-shell">
      <section className="game-panel">
        <Typography as="h1" variant="title-1">
          GraphRAG Online Movie Quiz
        </Typography>

        {error && (
          <Banner variant="danger" hasIcon isAlert>
            <Banner.Header>Something went wrong</Banner.Header>
            <Banner.Description>{error}</Banner.Description>
          </Banner>
        )}

        {isLoading && !game && finalScore === null ? (
          <div role="status">
            <Typography variant="body-large">Loading game…</Typography>
          </div>
        ) : game ? (
          <GameScreen game={game} isLoading={isLoading} onAnswer={handleAnswer} />
        ) : finalScore !== null ? (
          <GameOverScreen
            score={finalScore}
            correctAnswer={correctAnswer}
            onRestart={handleRestart}
          />
        ) : (
          <StartScreen
            isLoading={isLoading}
            onStart={handleStart}
          />
        )}
      </section>
    </main>
  )
}

interface StartScreenProps {
  isLoading: boolean
  onStart: (player: string) => Promise<void>
}

function StartScreen({ isLoading, onStart }: StartScreenProps) {
  const [player, setPlayer] = useState('')

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (player.trim()) void onStart(player)
  }

  return (
    <div className="start-screen">
      <Typography variant="body-large">
        Enter your name to start the quiz.
      </Typography>

      <form className="start-form" onSubmit={handleSubmit}>
        <TextInput
          label="Player name"
          value={player}
          onChange={(event) => setPlayer(event.target.value)}
          isDisabled={isLoading}
          isFluid
          isRequired
          htmlAttributes={{ autoComplete: 'name', maxLength: 50 }}
        />
        <FilledButton
          type="submit"
          isDisabled={!player.trim()}
          isLoading={isLoading}
          loadingMessage="Starting game"
        >
          Start game
        </FilledButton>
      </form>
    </div>
  )
}

function GameOverScreen({
  score,
  correctAnswer,
  onRestart,
}: {
  score: number
  correctAnswer: string | null
  onRestart: () => void
}) {
  return (
    <div className="game-over-screen">
      <Typography as="h2" variant="title-2">
        Game over
      </Typography>
      <Typography variant="title-3">Final score: {score}</Typography>
      {correctAnswer && (
        <Typography variant="body-large">
          The correct answer was {correctAnswer}.
        </Typography>
      )}
      <FilledButton type="button" onClick={onRestart}>
        Play again
      </FilledButton>
    </div>
  )
}

interface GameScreenProps {
  game: Game
  isLoading: boolean
  onAnswer: (name: string) => Promise<void>
}

function GameScreen({ game, isLoading, onAnswer }: GameScreenProps) {
  const [answer, setAnswer] = useState('')
  const [suggestions, setSuggestions] = useState<ActorSuggestion[]>([])
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false)

  useEffect(() => {
    if (answer.length < 2) return

    const controller = new AbortController()
    const timer = window.setTimeout(async () => {
      setIsLoadingSuggestions(true)
      try {
        setSuggestions(await findActorSuggestions(answer, controller.signal))
      } catch (requestError) {
        if (!(requestError instanceof DOMException && requestError.name === 'AbortError')) {
          setSuggestions([])
        }
      } finally {
        if (!controller.signal.aborted) setIsLoadingSuggestions(false)
      }
    }, 200)

    return () => {
      window.clearTimeout(timer)
      controller.abort()
    }
  }, [answer])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (!answer.trim()) return
    await onAnswer(answer)
    setAnswer('')
    setSuggestions([])
  }

  return (
    <div className="game-screen">
      <Typography variant="body-large">
        Score (completed questions): {game.score}
      </Typography>

      <Typography as="h2" variant="title-3">
        Who in {game.question.movie} starred in another movie with{' '}
        {game.question.person}?
      </Typography>

      <form className="answer-form" onSubmit={handleSubmit}>
        <TextInput
          label="Your answer"
          value={answer}
          onChange={(event) => {
            const value = event.target.value
            setAnswer(value)
            if (value.length < 2) {
              setSuggestions([])
              setIsLoadingSuggestions(false)
            }
          }}
          isDisabled={isLoading}
          isFluid
          isLoading={isLoadingSuggestions}
          htmlAttributes={{ autoComplete: 'off', autoFocus: true }}
        />
        <FilledButton
          type="submit"
          isDisabled={!answer.trim()}
          isLoading={isLoading}
          loadingMessage="Checking answer"
        >
          Submit answer
        </FilledButton>
      </form>

      {suggestions.length > 0 && (
        <div className="suggestions" aria-label="Actor suggestions">
          {suggestions.map((suggestion) => (
            <TextButton
              key={suggestion.name}
              type="button"
              variant="neutral"
              onClick={() => {
                setAnswer(suggestion.name)
                setSuggestions([])
              }}
            >
              {suggestion.name}
            </TextButton>
          ))}
        </div>
      )}
    </div>
  )
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'The request failed.'
}

export default App
