package com.graphragmoviequiz.api.question;

import java.util.Optional;
import java.util.UUID;

public interface ClueRepository {

    Optional<Clue> findQuestionMovieClue(UUID gameId);

    Optional<Clue> findConnectionMovieClue(UUID gameId);
}
