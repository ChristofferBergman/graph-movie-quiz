package com.graphragmoviequiz.api.game;

import com.graphragmoviequiz.api.question.CurrentQuestion;
import com.graphragmoviequiz.api.question.Clue;
import com.graphragmoviequiz.api.question.ClueRepository;
import com.graphragmoviequiz.api.question.QuestionRepository;
import com.graphragmoviequiz.api.highscore.HighScoreRepository;
import com.graphragmoviequiz.api.web.model.TokenType;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    @Mock
    private ClueRepository clues;

    @Mock
    private HighScoreRepository highScores;

    @Test
    void createsGameAndFirstQuestion() {
        var game = gameWithScore(0);
        var question = new CurrentQuestion("Rogue One", "Robert Duvall", "Open Range");
        when(games.create("Chris")).thenReturn(game);
        when(questions.replaceForGame(game.id())).thenReturn(question);
        var service = service();

        var response = service.createGame("  Chris  ");

        assertThat(response.id()).isEqualTo(game.id());
        assertThat(response.player()).isEqualTo("Chris");
        assertThat(response.question().movie()).isEqualTo("Rogue One");
        assertThat(response.question().person()).isEqualTo("Robert Duvall");
    }

    @Test
    void correctAnswerIncrementsScoreAndReturnsNextQuestion() {
        var game = gameWithScore(2);
        var nextQuestion = new CurrentQuestion("Arrival", "Jeremy Renner", "The Hurt Locker");
        when(games.findById(game.id())).thenReturn(Optional.of(game));
        when(questions.findAnswerForGame(game.id())).thenReturn(Optional.of("Diego Luna"));
        when(games.update(any(Game.class))).thenAnswer(invocation ->
                Optional.of(invocation.getArgument(0, Game.class))
        );
        when(questions.replaceForGame(game.id())).thenReturn(nextQuestion);
        var service = service();

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
    void incorrectAnswerEndsGameAndChecksHighScoreQualification() {
        var game = gameWithScore(2);
        when(games.findById(game.id())).thenReturn(Optional.of(game));
        when(questions.findAnswerForGame(game.id())).thenReturn(Optional.of("Diego Luna"));
        var service = service();

        var response = service.submitAnswer(game.id(), "Someone Else");

        assertThat(response.correct()).isFalse();
        assertThat(response.score()).isEqualTo(2);
        assertThat(response.correctAnswer()).isEqualTo("Diego Luna");
        assertThat(response.game()).isNull();
        verify(highScores).finishGame(game.id());
        verify(questions, never()).replaceForGame(game.id());
    }

    @Test
    void usesGraphRagTokenAndReturnsPersistentQuestionState() {
        var game = gameWithScore(2);
        var updated = new Game(
                game.id(), game.player(), game.score(), 2, 1, false, true, game.lastActivity()
        );
        when(games.useToken(game.id(), HelpTokenType.GRAPH_RAG)).thenReturn(Optional.of(
                new TokenUseResult(TokenUseResult.Status.APPLIED, updated)
        ));
        when(questions.findForGame(game.id())).thenReturn(Optional.of(
                new CurrentQuestion("Rogue One", "Robert Duvall", "Open Range")
        ));

        var response = service().useToken(game.id(), TokenType.GRAPH_RAG);

        assertThat(response.remainingGraphRag()).isEqualTo(1);
        assertThat(response.question().graphRagUsed()).isTrue();
        assertThat(response.question().connectionMovie()).isEqualTo("Open Range");
    }

    @Test
    void rejectsRagAfterGraphRagWasUsed() {
        var game = gameWithScore(2);
        when(games.useToken(game.id(), HelpTokenType.RAG)).thenReturn(Optional.of(
                new TokenUseResult(TokenUseResult.Status.NOT_ALLOWED, game)
        ));

        assertThatThrownBy(() -> service().useToken(game.id(), TokenType.RAG))
                .hasMessage("RAG cannot be used after GraphRAG on the same question.");
    }

    @Test
    void repeatedTokenUseReturnsCurrentStateWithoutConsumingAgain() {
        var game = new Game(
                UUID.randomUUID(), "Chris", 2, 1, 2, true, false,
                ZonedDateTime.of(2026, 8, 31, 10, 0, 0, 0, ZoneOffset.UTC)
        );
        when(games.useToken(game.id(), HelpTokenType.RAG)).thenReturn(Optional.of(
                new TokenUseResult(TokenUseResult.Status.ALREADY_USED, game)
        ));
        when(questions.findForGame(game.id())).thenReturn(Optional.of(
                new CurrentQuestion("Rogue One", "Robert Duvall", "Open Range")
        ));

        var response = service().useToken(game.id(), TokenType.RAG);

        assertThat(response.remainingRag()).isEqualTo(1);
        assertThat(response.question().ragUsed()).isTrue();
    }

    @Test
    void returnsUnlockedQuestionClue() {
        var game = gameWithScore(2);
        when(games.findById(game.id())).thenReturn(Optional.of(game));
        when(clues.findQuestionMovieClue(game.id())).thenReturn(Optional.of(
                new Clue("Rogue One", java.util.List.of("Felicity Jones", "Diego Luna"))
        ));

        var clue = service().getQuestionClue(game.id());

        assertThat(clue.movie()).isEqualTo("Rogue One");
        assertThat(clue.actors()).containsExactly("Felicity Jones", "Diego Luna");
    }

    private GameService service() {
        return new GameService(games, questions, clues, highScores);
    }

    private Game gameWithScore(int score) {
        return new Game(
                UUID.randomUUID(),
                "Chris",
                score,
                2,
                2,
                false,
                false,
                ZonedDateTime.of(2026, 8, 31, 10, 0, 0, 0, ZoneOffset.UTC)
        );
    }
}
