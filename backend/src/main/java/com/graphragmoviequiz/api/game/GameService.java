package com.graphragmoviequiz.api.game;

import com.graphragmoviequiz.api.error.GameNotFoundException;
import com.graphragmoviequiz.api.error.ApiException;
import com.graphragmoviequiz.api.question.ClueRepository;
import com.graphragmoviequiz.api.question.CurrentQuestion;
import com.graphragmoviequiz.api.question.QuestionRepository;
import com.graphragmoviequiz.api.highscore.HighScoreRepository;
import com.graphragmoviequiz.api.web.model.GameResponse;
import com.graphragmoviequiz.api.web.model.ClueResponse;
import com.graphragmoviequiz.api.web.model.QuestionResponse;
import com.graphragmoviequiz.api.web.model.SubmitAnswerResponse;
import com.graphragmoviequiz.api.web.model.TokenType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class GameService {

    private static final int QUESTION_TIME_LIMIT_SECONDS = 40;

    private final GameRepository games;
    private final QuestionRepository questions;
    private final ClueRepository clues;
    private final HighScoreRepository highScores;

    public GameService(
            GameRepository games,
            QuestionRepository questions,
            ClueRepository clues,
            HighScoreRepository highScores
    ) {
        this.games = games;
        this.questions = questions;
        this.clues = clues;
        this.highScores = highScores;
    }

    public GameResponse createGame(String player) {
        var game = games.create(player.trim());
        var question = questions.replaceForGame(game.id());
        game = touch(game);
        return toResponse(game, question);
    }

    public GameResponse getGame(UUID gameId) {
        var game = getExistingGame(gameId);
        var question = questions.findForGame(gameId)
                .orElseThrow(() -> new IllegalStateException("Game has no current question."));
        return toResponse(game, question);
    }

    public void closeGame(UUID gameId) {
        getExistingGame(gameId);
        games.deleteById(gameId);
    }

    public SubmitAnswerResponse submitAnswer(UUID gameId, String name) {
        var game = getExistingGame(gameId);
        var correctAnswer = questions.findAnswerForGame(gameId)
                .orElseThrow(() -> new IllegalStateException("Game has no current answer."));

        if (isExpired(game) || !correctAnswer.equalsIgnoreCase(name)) {
            return finishGame(game, correctAnswer);
        }

        var nextQuestion = questions.replaceForGame(gameId);
        var updatedGame = games.update(new Game(
                game.id(),
                game.player(),
                game.score() + 1,
                game.remainingRag(),
                game.remainingGraphRag(),
                false,
                false,
                ZonedDateTime.now(ZoneOffset.UTC)
        ))
                .orElseThrow(() -> new GameNotFoundException(gameId));

        return new SubmitAnswerResponse(
                true,
                updatedGame.score(),
                null,
                toResponse(updatedGame, nextQuestion)
        );
    }

    public SubmitAnswerResponse timeoutGame(UUID gameId) {
        var game = getExistingGame(gameId);
        var correctAnswer = questions.findAnswerForGame(gameId)
                .orElseThrow(() -> new IllegalStateException("Game has no current answer."));
        return finishGame(game, correctAnswer);
    }

    public GameResponse useToken(UUID gameId, TokenType type) {
        var result = games.useToken(gameId, HelpTokenType.valueOf(type.name()))
                .orElseThrow(() -> new GameNotFoundException(gameId));

        if (result.status() == TokenUseResult.Status.NOT_ALLOWED) {
            throw conflict("RAG cannot be used after GraphRAG on the same question.");
        }
        if (result.status() == TokenUseResult.Status.NO_TOKENS) {
            throw conflict("No tokens of this type remain.");
        }

        var question = questions.findForGame(gameId)
                .orElseThrow(() -> new IllegalStateException("Game has no current question."));
        return toResponse(result.game(), question);
    }

    public ClueResponse getQuestionClue(UUID gameId) {
        getExistingGame(gameId);
        return clues.findQuestionMovieClue(gameId)
                .map(clue -> new ClueResponse(clue.movie(), clue.actors()))
                .orElseThrow(() -> conflict("The question movie clue has not been unlocked."));
    }

    public ClueResponse getConnectionClue(UUID gameId) {
        getExistingGame(gameId);
        return clues.findConnectionMovieClue(gameId)
                .map(clue -> new ClueResponse(clue.movie(), clue.actors()))
                .orElseThrow(() -> conflict("The connection movie clue has not been unlocked."));
    }

    private Game getExistingGame(UUID gameId) {
        return games.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
    }

    private GameResponse toResponse(Game game, CurrentQuestion question) {
        return new GameResponse(
                game.id(),
                game.player(),
                game.score(),
                game.remainingRag(),
                game.remainingGraphRag(),
                game.lastActivity().plusSeconds(QUESTION_TIME_LIMIT_SECONDS),
                new QuestionResponse(
                        question.movie(),
                        question.person(),
                        game.graphRagUsed() ? question.connectionMovie() : null,
                        game.ragUsed(),
                        game.graphRagUsed()
                )
        );
    }

    private Game touch(Game game) {
        return games.update(new Game(
                game.id(),
                game.player(),
                game.score(),
                game.remainingRag(),
                game.remainingGraphRag(),
                game.ragUsed(),
                game.graphRagUsed(),
                ZonedDateTime.now(ZoneOffset.UTC)
        )).orElseThrow(() -> new GameNotFoundException(game.id()));
    }

    private boolean isExpired(Game game) {
        return !ZonedDateTime.now(ZoneOffset.UTC)
                .isBefore(game.lastActivity().plusSeconds(QUESTION_TIME_LIMIT_SECONDS));
    }

    private SubmitAnswerResponse finishGame(Game game, String correctAnswer) {
        highScores.finishGame(game.id());
        return new SubmitAnswerResponse(false, game.score(), correctAnswer, null);
    }

    private ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, "Action not allowed", detail);
    }
}
