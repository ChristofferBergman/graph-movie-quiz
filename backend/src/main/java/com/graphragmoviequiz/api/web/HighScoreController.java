package com.graphragmoviequiz.api.web;

import com.graphragmoviequiz.api.highscore.HighScoreService;
import com.graphragmoviequiz.api.web.model.HighScoreEntryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/high-scores")
public class HighScoreController {

    private final HighScoreService highScoreService;

    public HighScoreController(HighScoreService highScoreService) {
        this.highScoreService = highScoreService;
    }

    @GetMapping
    List<HighScoreEntryResponse> getHighScores() {
        return highScoreService.getHighScores();
    }
}
