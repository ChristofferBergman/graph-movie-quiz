package com.graphragmoviequiz.api.error;

import org.springframework.http.HttpStatus;

import java.util.UUID;

public class GameNotFoundException extends ApiException {

    public GameNotFoundException(UUID gameId) {
        super(HttpStatus.NOT_FOUND, "Game not found", "No active game exists with id " + gameId + ".");
    }
}
