package de.quantumquest.triviathrone.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
    @GetMapping("/")
    public String root() {
        return "forward:/index.html";
    }

    @GetMapping("/login")
    public String login() {
        return "forward:/login.html";
    }

    @GetMapping("/lobby")
    public String lobby() {
        return "forward:/lobby.html";
    }

    @GetMapping("/game")
    public String game() {
        return "forward:/game.html";
    }

    @GetMapping("/editor")
    public String editor() {
        return "forward:/editor.html";
    }

    @GetMapping("/account")
    public String account() {
        return "redirect:/?page=account";
    }
}
