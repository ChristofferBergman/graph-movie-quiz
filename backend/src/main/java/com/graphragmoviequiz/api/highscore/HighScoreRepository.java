package com.graphragmoviequiz.api.highscore;

import java.util.List;
import java.util.UUID;

public interface HighScoreRepository {

    void finishGame(UUID gameId);

    List<HighScoreEntry> findTopThree();
}
