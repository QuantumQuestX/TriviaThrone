package quantumquest.triviathrone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Starts the TriviaThrone Spring Boot application.
 *
 * @author TiJ
 * @since 1.0.0
 */
@SpringBootApplication
@EnableScheduling
public class TriviaThroneApplication {

    /**
     * Launches the application with the supplied command-line arguments.
     *
     * @param args command-line arguments passed to Spring Boot
     */
    public static void main(String[] args) {
        SpringApplication.run(TriviaThroneApplication.class, args);
    }
}
