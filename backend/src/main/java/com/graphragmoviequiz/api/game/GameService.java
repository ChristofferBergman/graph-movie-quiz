package com.graphragmoviequiz.api.game;

import com.graphragmoviequiz.api.error.GameNotFoundException;
import com.graphragmoviequiz.api.question.CurrentQuestion;
import com.graphragmoviequiz.api.question.QuestionRepository;
import com.graphragmoviequiz.api.web.model.GameResponse;
import com.graphragmoviequiz.api.web.model.QuestionResponse;
import com.graphragmoviequiz.api.web.model.SubmitAnswerResponse;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

@Service
public class GameService {

    private final GameRepository games;
    private final QuestionRepository questions;

    public GameService(GameRepository games, QuestionRepository questions) {
        this.games = games;
        this.questions = questions;
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
                ZonedDateTime.now(ZoneOffset.UTC)
        );
        updatedGame = games.update(updatedGame)
                .orElseThrow(() -> new GameNotFoundException(gameId));
        var nextQuestion = questions.replaceForGame(gameId);

        return new SubmitAnswerResponse(
                true,
                updatedGame.score(),
                null,
                toResponse(updatedGame, nextQuestion)
        );
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
                new QuestionResponse(question.movie(), question.person(), false, false)
        );
    }
}
