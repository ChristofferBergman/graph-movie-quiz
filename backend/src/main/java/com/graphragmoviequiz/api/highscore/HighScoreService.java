package com.graphragmoviequiz.api.highscore;

import com.graphragmoviequiz.api.web.model.HighScoreEntryResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HighScoreService {

    private final HighScoreRepository highScores;

    public HighScoreService(HighScoreRepository highScores) {
        this.highScores = highScores;
    }

    public List<HighScoreEntryResponse> getHighScores() {
        return highScores.findTopThree().stream()
                .map(entry -> new HighScoreEntryResponse(entry.player(), entry.score()))
                .toList();
    }
}
