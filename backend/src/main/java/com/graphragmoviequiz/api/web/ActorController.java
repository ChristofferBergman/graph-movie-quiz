package com.graphragmoviequiz.api.web;

import com.graphragmoviequiz.api.actor.ActorRepository;
import com.graphragmoviequiz.api.web.model.ActorSuggestionResponse;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/v1/actors")
public class ActorController {

    private final ActorRepository actors;

    public ActorController(ActorRepository actors) {
        this.actors = actors;
    }

    @GetMapping("/suggestions")
    List<ActorSuggestionResponse> findSuggestions(
            @RequestParam
            @Size(min = 2, message = "Prefix must contain at least two characters.")
            String prefix
    ) {
        return actors.findSuggestions(prefix).stream()
                .map(ActorSuggestionResponse::new)
                .toList();
    }
}
