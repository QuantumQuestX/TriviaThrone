package de.quantumquest.triviathrone.config;

import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VersionController {
    private final BuildProperties buildProperties;
    private final ProjectDeveloperService projectDeveloperService;

    public VersionController(
            BuildProperties buildProperties,
            ProjectDeveloperService projectDeveloperService
    ) {
        this.buildProperties = buildProperties;
        this.projectDeveloperService = projectDeveloperService;
    }

    @GetMapping(value = "/api/version", produces = MediaType.TEXT_PLAIN_VALUE)
    public String version() {
        return buildProperties.getVersion();
    }

    @GetMapping(value = "/api/developers", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectDeveloperService.Developer> developers() {
        return projectDeveloperService.getDevelopers();
    }
}
