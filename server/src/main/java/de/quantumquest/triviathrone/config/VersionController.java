package de.quantumquest.triviathrone.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VersionController {
    private final String version;
    private final ProjectDeveloperService projectDeveloperService;

    public VersionController(
            ObjectProvider<BuildProperties> buildProperties,
            ProjectDeveloperService projectDeveloperService
    ) {
        BuildProperties properties = buildProperties.getIfAvailable();
        this.version = properties == null || properties.getVersion().isBlank()
                ? packageVersion()
                : properties.getVersion();
        this.projectDeveloperService = projectDeveloperService;
    }

    @GetMapping(value = "/api/version", produces = MediaType.TEXT_PLAIN_VALUE)
    public String version() {
        return version;
    }

    @GetMapping(value = "/api/developers", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectDeveloperService.Developer> developers() {
        return projectDeveloperService.getDevelopers();
    }

    private static String packageVersion() {
        String version = VersionController.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "development" : version;
    }
}
