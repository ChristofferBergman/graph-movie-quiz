package com.graphragmoviequiz.api.game;

import com.graphragmoviequiz.api.question.CurrentQuestion;
import com.graphragmoviequiz.api.question.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTests {

    @Mock
    private GameRepository games;

    @Mock
    private QuestionRepository questions;

    @Test
    void createsGameAndFirstQuestion() {
        var game = gameWithScore(0);
        var question = new CurrentQuestion("Rogue One", "Robert Duvall");
        when(games.create("Chris")).thenReturn(game);
        when(questions.replaceForGame(game.id())).thenReturn(question);
        var service = new GameService(games, questions);

        var response = service.createGame("  Chris  ");

        assertThat(response.id()).isEqualTo(game.id());
        assertThat(response.player()).isEqualTo("Chris");
        assertThat(response.question().movie()).isEqualTo("Rogue One");
        assertThat(response.question().person()).isEqualTo("Robert Duvall");
    }

    @Test
    void correctAnswerIncrementsScoreAndReturnsNextQuestion() {
        var game = gameWithScore(2);
        var nextQuestion = new CurrentQuestion("Arrival", "Jeremy Renner");
        when(games.findById(game.id())).thenReturn(Optional.of(game));
        when(questions.findAnswerForGame(game.id())).thenReturn(Optional.of("Diego Luna"));
        when(games.update(any(Game.class))).thenAnswer(invocation ->
                Optional.of(invocation.getArgument(0, Game.class))
        );
        when(questions.replaceForGame(game.id())).thenReturn(nextQuestion);
        var service = new GameService(games, questions);

        var response = service.submitAnswer(game.id(), "Diego Luna");

        assertThat(response.correct()).isTrue();
        assertThat(response.score()).isEqualTo(3);
        assertThat(response.correctAnswer()).isNull();
        assertThat(response.game().question().movie()).isEqualTo("Arrival");
        var updatedGame = ArgumentCaptor.forClass(Game.class);
        verify(games).update(updatedGame.capture());
        assertThat(updatedGame.getValue().score()).isEqualTo(3);
        assertThat(updatedGame.getValue().lastActivity()).isAfter(game.lastActivity());
    }

    @Test
    void incorrectAnswerEndsAndDeletesGame() {
        var game = gameWithScore(2);
        when(games.findById(game.id())).thenReturn(Optional.of(game));
        when(questions.findAnswerForGame(game.id())).thenReturn(Optional.of("Diego Luna"));
        var service = new GameService(games, questions);

        var response = service.submitAnswer(game.id(), "Someone Else");

        assertThat(response.correct()).isFalse();
        assertThat(response.score()).isEqualTo(2);
        assertThat(response.correctAnswer()).isEqualTo("Diego Luna");
        assertThat(response.game()).isNull();
        verify(games).deleteById(game.id());
        verify(questions, never()).replaceForGame(game.id());
    }

    private Game gameWithScore(int score) {
        return new Game(
                UUID.randomUUID(),
                "Chris",
                score,
                2,
                2,
                ZonedDateTime.of(2026, 8, 31, 10, 0, 0, 0, ZoneOffset.UTC)
        );
    }
}
