package de.quantumquest.triviathrone.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {
    @GetMapping("/")
    public String root() {
        return "redirect:/index.html";
    }

    @GetMapping("/account")
    public String account() {
        return "redirect:/index.html?page=account";
    }
}
