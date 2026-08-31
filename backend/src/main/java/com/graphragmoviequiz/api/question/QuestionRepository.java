package com.graphragmoviequiz.api.question;

import java.util.Optional;
import java.util.UUID;

public interface QuestionRepository {

    CurrentQuestion replaceForGame(UUID gameId);

    Optional<CurrentQuestion> findForGame(UUID gameId);

    Optional<String> findAnswerForGame(UUID gameId);
}
