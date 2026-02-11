package de.quantumquest.triviathrone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class TriviaThroneApplication {

    static void main(String[] args) {
        SpringApplication.run(TriviaThroneApplication.class, args);
    }

}
