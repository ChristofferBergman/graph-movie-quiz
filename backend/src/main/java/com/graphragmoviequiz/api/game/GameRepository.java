package com.graphragmoviequiz.api.game;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository {

    Game create(String player);

    Optional<Game> findById(UUID id);

    Optional<Game> update(Game game);

    Optional<TokenUseResult> useToken(UUID id, HelpTokenType type);

    void deleteById(UUID id);
}
