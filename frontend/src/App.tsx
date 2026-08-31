import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type MouseEvent,
} from 'react'
import {
  Banner,
  FilledButton,
  Logo,
  TextButton,
  TextInput,
  Typography,
} from '@neo4j-ndl/react'
import {
  createGame,
  closeGame,
  findActorSuggestions,
  loadGame,
  loadHighScores,
  loadClue,
  submitAnswer,
  consumeHelpToken,
  ApiError,
  type ActorSuggestion,
  type Clue,
  type ClueType,
  type Game,
  type HighScoreEntry,
  type TokenType,
} from './api'
import clueBack from './assets/images/ClueBack.png'
import clueFront from './assets/images/ClueFront.png'
import './App.css'

const GAME_ID_STORAGE_KEY = 'graphrag-movie-quiz.game-id'

function App() {
  const [game, setGame] = useState<Game | null>(null)
  const [finalScore, setFinalScore] = useState<number | null>(null)
  const [correctAnswer, setCorrectAnswer] = useState<string | null>(null)
  const [highScores, setHighScores] = useState<HighScoreEntry[]>([])
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
      .then(async (loadedGame) => {
        const loadedHighScores = await getHighScoresSafely()
        if (isActive) {
          setGame(loadedGame)
          setHighScores(loadedHighScores)
        }
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
      const loadedHighScores = await getHighScoresSafely()
      localStorage.setItem(GAME_ID_STORAGE_KEY, createdGame.id)
      setGame(createdGame)
      setHighScores(loadedHighScores)
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
        setHighScores(await getHighScoresSafely())
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

  async function handleToken(type: TokenType) {
    if (!game || requestInFlight.current) return
    requestInFlight.current = true
    setError(null)
    setIsLoading(true)

    try {
      setGame(await consumeHelpToken(game.id, type))
    } catch (requestError) {
      setError(getErrorMessage(requestError))
    } finally {
      requestInFlight.current = false
      setIsLoading(false)
    }
  }

  async function handleCloseGame() {
    if (!game || requestInFlight.current) return
    requestInFlight.current = true
    setError(null)
    setIsLoading(true)

    try {
      await closeGame(game.id)
      localStorage.removeItem(GAME_ID_STORAGE_KEY)
      setGame(null)
    } catch (requestError) {
      if (requestError instanceof ApiError && requestError.status === 404) {
        localStorage.removeItem(GAME_ID_STORAGE_KEY)
        setGame(null)
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
          <GameScreen
            key={`${game.score}-${game.question.movie}-${game.question.person}`}
            game={game}
            isLoading={isLoading}
            onAnswer={handleAnswer}
            onUseToken={handleToken}
            onClose={handleCloseGame}
            highScores={highScores}
          />
        ) : finalScore !== null ? (
          <div className="game-layout game-over-layout">
            <HighScorePanel entries={highScores} />
            <GameOverScreen
              score={finalScore}
              correctAnswer={correctAnswer}
              onRestart={handleRestart}
            />
            <div aria-hidden="true" />
          </div>
        ) : (
          <StartScreen
            isLoading={isLoading}
            onStart={handleStart}
          />
        )}
      </section>
      <footer className="neo4j-footer" aria-label="Powered by Neo4j">
        <Logo type="full" color="color" />
      </footer>
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
  onUseToken: (type: TokenType) => Promise<void>
  onClose: () => Promise<void>
  highScores: HighScoreEntry[]
}

function GameScreen({
  game,
  isLoading,
  onAnswer,
  onUseToken,
  onClose,
  highScores,
}: GameScreenProps) {
  const [answer, setAnswer] = useState('')
  const [suggestions, setSuggestions] = useState<ActorSuggestion[]>([])
  const [isLoadingSuggestions, setIsLoadingSuggestions] = useState(false)
  const suppressNextSuggestionLookup = useRef(false)

  useEffect(() => {
    if (suppressNextSuggestionLookup.current) {
      suppressNextSuggestionLookup.current = false
      return
    }
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
    <div className="game-layout">
      <HighScorePanel entries={highScores} />
      <div className="game-screen">
        <Typography variant="body-large">
          Score (completed questions): {game.score}
        </Typography>

        <Typography as="h2" variant="title-3">
          Who in {game.question.movie} starred in another movie with{' '}
          {game.question.person}?
        </Typography>

        <div className="answer-area">
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
                    suppressNextSuggestionLookup.current = true
                    setAnswer(suggestion.name)
                    setSuggestions([])
                    setIsLoadingSuggestions(false)
                  }}
                >
                  {suggestion.name}
                </TextButton>
              ))}
            </div>
          )}
        </div>

        <QuestionGraph game={game} />
        <Typography variant="body-small" className="zoom-hint">
          Right-click card to zoom
        </Typography>
        <TextButton
          type="button"
          variant="neutral"
          isDisabled={isLoading}
          onClick={() => void onClose()}
        >
          Close game
        </TextButton>
      </div>
      <TokenPanel game={game} isLoading={isLoading} onUseToken={onUseToken} />
    </div>
  )
}

function HighScorePanel({ entries }: { entries: HighScoreEntry[] }) {
  return (
    <aside className="high-score-panel" aria-label="High scores">
      <Typography variant="body-medium">High scores</Typography>
      {entries.length === 0 ? (
        <Typography variant="body-small">No scores yet</Typography>
      ) : (
        <ol className="high-score-list">
          {entries.map((entry, index) => (
            <li key={`${entry.player}-${entry.score}-${index}`}>
              <span className="high-score-rank">{index + 1}</span>
              <span className="high-score-player">{entry.player}</span>
              <span>{entry.score}</span>
            </li>
          ))}
        </ol>
      )}
    </aside>
  )
}

function TokenPanel({
  game,
  isLoading,
  onUseToken,
}: {
  game: Game
  isLoading: boolean
  onUseToken: (type: TokenType) => Promise<void>
}) {
  return (
    <aside className="token-panel" aria-label="Help tokens">
      <Typography variant="body-medium">Help tokens</Typography>
      <div className="token-list">
        <TokenColumn
          type="RAG"
          remaining={game.remainingRag}
          unavailable={game.question.ragUsed || game.question.graphRagUsed}
          isLoading={isLoading}
          onUseToken={onUseToken}
        />
        <TokenColumn
          type="GRAPH_RAG"
          remaining={game.remainingGraphRag}
          unavailable={game.question.graphRagUsed}
          isLoading={isLoading}
          onUseToken={onUseToken}
        />
      </div>
    </aside>
  )
}

function TokenColumn({
  type,
  remaining,
  unavailable,
  isLoading,
  onUseToken,
}: {
  type: TokenType
  remaining: number
  unavailable: boolean
  isLoading: boolean
  onUseToken: (type: TokenType) => Promise<void>
}) {
  const label = type === 'RAG' ? 'RAG' : 'GraphRAG'

  return (
    <div className="token-column">
      {[0, 1].map((index) => {
        const isSpent = index < 2 - remaining
        const isUnavailable = !isSpent && unavailable
        return (
          <button
            key={index}
            className={`help-token help-token--${type === 'RAG' ? 'rag' : 'graph'} ${isSpent ? 'help-token--spent' : ''} ${isUnavailable ? 'help-token--unavailable' : ''}`}
            type="button"
            disabled={isLoading || isSpent || isUnavailable}
            onClick={() => void onUseToken(type)}
            aria-label={
              isSpent
                ? `${label} token ${index + 1}, used`
                : `Use ${label} token ${index + 1}`
            }
          >
            {!isSpent && <span>{label}</span>}
          </button>
        )
      })}
    </div>
  )
}

function QuestionGraph({ game }: { game: Game }) {
  const questionUnlocked = game.question.ragUsed || game.question.graphRagUsed

  return (
    <section className="question-graph" aria-label="Question graph">
      <StaticClueCard label={game.question.person} />
      <GraphConnector />
      {game.question.graphRagUsed ? (
        <ClueCard
          gameId={game.id}
          type="connection"
          label={game.question.connectionMovie ?? 'Connection movie'}
        />
      ) : (
        <div className="connection-placeholder" aria-label="Hidden connection movie" />
      )}
      <GraphConnector />
      <GraphPerson />
      <GraphConnector />
      <ClueCard
        gameId={game.id}
        type="question"
        label={game.question.movie}
        isLocked={!questionUnlocked}
      />
    </section>
  )
}

function GraphConnector() {
  return <div className="graph-connector" aria-hidden="true" />
}

function GraphPerson() {
  return (
    <div className="graph-person" aria-label="Unknown person">
      ?
    </div>
  )
}

function StaticClueCard({ label }: { label: string }) {
  return (
    <div className="clue-card clue-card--locked" aria-label={`${label} clue locked`}>
      <img src={clueBack} alt="" />
      <span className="clue-card__label">{label}</span>
    </div>
  )
}

function ClueCard({
  gameId,
  type,
  label,
  isLocked = false,
}: {
  gameId: string
  type: ClueType
  label?: string
  isLocked?: boolean
}) {
  const [clue, setClue] = useState<Clue | null>(null)
  const [isFlipped, setIsFlipped] = useState(false)
  const [isLoadingClue, setIsLoadingClue] = useState(false)
  const [isZoomed, setIsZoomed] = useState(false)
  const [clueError, setClueError] = useState<string | null>(null)
  const loadingRef = useRef(false)

  async function handleFlip(event: MouseEvent<HTMLButtonElement>) {
    if (event.detail > 1 || isLocked || loadingRef.current) return
    if (clue) {
      setIsFlipped((current) => !current)
      return
    }

    loadingRef.current = true
    setIsLoadingClue(true)
    setClueError(null)
    try {
      setClue(await loadClue(gameId, type))
      setIsFlipped(true)
    } catch (requestError) {
      setClueError(getErrorMessage(requestError))
    } finally {
      loadingRef.current = false
      setIsLoadingClue(false)
    }
  }

  function handleContextMenu(event: MouseEvent<HTMLButtonElement>) {
    if (!isFlipped || !clue) return
    event.preventDefault()
    setIsZoomed(true)
  }

  const displayLabel = clue?.movie ?? label ?? 'Connection movie'

  return (
    <>
      <button
        className={`clue-card ${isLocked ? 'clue-card--locked' : ''} ${isFlipped ? 'clue-card--flipped' : ''}`}
        type="button"
        disabled={isLocked || isLoadingClue}
        onClick={handleFlip}
        onDoubleClick={(event) => event.preventDefault()}
        onContextMenu={handleContextMenu}
        aria-label={`${displayLabel} clue${isLocked ? ' locked' : ''}`}
        aria-pressed={isFlipped}
      >
        <span className="clue-card__inner">
          <span className="clue-card__face clue-card__back">
            <img src={clueBack} alt="" />
            <span className="clue-card__label">
              {isLoadingClue ? 'Loading…' : displayLabel}
            </span>
          </span>
          <span className="clue-card__face clue-card__front">
            <img src={clueFront} alt="" />
            {clue && <ClueDetails clue={clue} />}
          </span>
        </span>
      </button>
      {clueError && <span className="clue-card__error">{clueError}</span>}
      {isZoomed && clue && (
        <div
          className="clue-dialog"
          role="dialog"
          aria-modal="true"
          aria-label={`${clue.movie} clue`}
          onPointerDown={() => setIsZoomed(false)}
          onContextMenu={(event) => {
            event.preventDefault()
            setIsZoomed(false)
          }}
        >
          <div className="clue-dialog__content">
            <div className="clue-card clue-card--zoomed clue-card--flipped">
              <span className="clue-card__inner">
                <span className="clue-card__face clue-card__front">
                  <img src={clueFront} alt="" />
                  <ClueDetails clue={clue} />
                </span>
              </span>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

function ClueDetails({ clue }: { clue: Clue }) {
  return (
    <span className="clue-details">
      <strong>{clue.movie}</strong>
      <span>Actors</span>
      <span className="clue-details__actors">{clue.actors.join('\n')}</span>
    </span>
  )
}

function getErrorMessage(error: unknown): string {
  return error instanceof Error ? error.message : 'The request failed.'
}

async function getHighScoresSafely(): Promise<HighScoreEntry[]> {
  try {
    return await loadHighScores()
  } catch {
    return []
  }
}

export default App
