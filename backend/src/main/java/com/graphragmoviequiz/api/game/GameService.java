package com.graphragmoviequiz.api.game;

import com.graphragmoviequiz.api.error.GameNotFoundException;
import com.graphragmoviequiz.api.error.ApiException;
import com.graphragmoviequiz.api.question.ClueRepository;
import com.graphragmoviequiz.api.question.CurrentQuestion;
import com.graphragmoviequiz.api.question.QuestionRepository;
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

    private final GameRepository games;
    private final QuestionRepository questions;
    private final ClueRepository clues;

    public GameService(GameRepository games, QuestionRepository questions, ClueRepository clues) {
        this.games = games;
        this.questions = questions;
        this.clues = clues;
    }

    public GameResponse createGame(String player) {
        var game = games.create(player.trim());
        var question = questions.replaceForGame(game.id());
        return toResponse(game, question);
    }

    public GameResponse getGame(UUID gameId) {
        var game = getExistingGame(gameId);
        var question = questions.findForGame(gameId)
                .orElseThrow(() -> new IllegalStateException("Game has no current question."));
        return toResponse(game, question);
    }

    public SubmitAnswerResponse submitAnswer(UUID gameId, String name) {
        var game = getExistingGame(gameId);
        var correctAnswer = questions.findAnswerForGame(gameId)
                .orElseThrow(() -> new IllegalStateException("Game has no current answer."));

        if (!correctAnswer.equalsIgnoreCase(name)) {
            games.deleteById(gameId);
            return new SubmitAnswerResponse(false, game.score(), correctAnswer, null);
        }

        var updatedGame = new Game(
                game.id(),
                game.player(),
                game.score() + 1,
                game.remainingRag(),
                game.remainingGraphRag(),
                game.ragUsed(),
                game.graphRagUsed(),
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        updatedGame = games.update(updatedGame)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        var nextQuestion = questions.replaceForGame(gameId);
        updatedGame = new Game(
                updatedGame.id(),
                updatedGame.player(),
                updatedGame.score(),
                updatedGame.remainingRag(),
                updatedGame.remainingGraphRag(),
                false,
                false,
                updatedGame.lastActivity()
        );

        return new SubmitAnswerResponse(
                true,
                updatedGame.score(),
                null,
                toResponse(updatedGame, nextQuestion)
        );
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
                new QuestionResponse(
                        question.movie(),
                        question.person(),
                        game.ragUsed(),
                        game.graphRagUsed()
                )
        );
    }

    private ApiException conflict(String detail) {
        return new ApiException(HttpStatus.CONFLICT, "Action not allowed", detail);
    }
}
