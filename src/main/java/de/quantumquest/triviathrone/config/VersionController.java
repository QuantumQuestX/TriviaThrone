package de.quantumquest.triviathrone.config;

import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class VersionController {
    private final BuildProperties buildProperties;

    public VersionController(BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @GetMapping(value = "/api/version", produces = MediaType.TEXT_PLAIN_VALUE)
    public String version() {
        return buildProperties.getVersion();
    }
}
