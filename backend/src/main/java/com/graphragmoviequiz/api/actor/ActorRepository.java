package com.graphragmoviequiz.api.actor;

import java.util.List;

public interface ActorRepository {

    List<String> findSuggestions(String prefix);
}
