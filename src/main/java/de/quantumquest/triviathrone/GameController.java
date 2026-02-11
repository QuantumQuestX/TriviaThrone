package de.quantumquest.triviathrone;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/game")
public class GameController {
    @GetMapping("/state")
    public String state(Authentication authentication) {
        return "Player " + authentication.getName() + " is alive";
    }
}
