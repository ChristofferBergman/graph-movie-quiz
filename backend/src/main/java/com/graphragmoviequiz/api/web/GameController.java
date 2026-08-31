package com.graphragmoviequiz.api.web;

import com.graphragmoviequiz.api.game.GameService;
import com.graphragmoviequiz.api.web.model.CreateGameRequest;
import com.graphragmoviequiz.api.web.model.GameResponse;
import com.graphragmoviequiz.api.web.model.SubmitAnswerRequest;
import com.graphragmoviequiz.api.web.model.SubmitAnswerResponse;
import com.graphragmoviequiz.api.web.model.ClueResponse;
import com.graphragmoviequiz.api.web.model.UseTokenRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    ResponseEntity<GameResponse> createGame(@Valid @RequestBody CreateGameRequest request) {
        var game = gameService.createGame(request.player());
        return ResponseEntity
                .created(URI.create("/api/v1/games/" + game.id()))
                .body(game);
    }

    @GetMapping("/{gameId}")
    GameResponse getGame(@PathVariable UUID gameId) {
        return gameService.getGame(gameId);
    }

    @DeleteMapping("/{gameId}")
    ResponseEntity<Void> closeGame(@PathVariable UUID gameId) {
        gameService.closeGame(gameId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{gameId}/answers")
    SubmitAnswerResponse submitAnswer(
            @PathVariable UUID gameId,
            @Valid @RequestBody SubmitAnswerRequest request
    ) {
        return gameService.submitAnswer(gameId, request.name());
    }

    @PostMapping("/{gameId}/tokens")
    GameResponse useToken(
            @PathVariable UUID gameId,
            @Valid @RequestBody UseTokenRequest request
    ) {
        return gameService.useToken(gameId, request.type());
    }

    @GetMapping("/{gameId}/clues/question")
    ClueResponse getQuestionClue(@PathVariable UUID gameId) {
        return gameService.getQuestionClue(gameId);
    }

    @GetMapping("/{gameId}/clues/connection")
    ClueResponse getConnectionClue(@PathVariable UUID gameId) {
        return gameService.getConnectionClue(gameId);
    }
}
