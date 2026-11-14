package quantumquest.triviathrone.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Exposes build and project-developer information.
 *
 * @author TiJ
 * @since 2.0.0
 */
@RestController
public class VersionController {
    /**
     * Holds the version used by this component.
     */
    private final String version;
    /**
     * Holds the projec evelope ervice used by this component.
     */
    private final ProjectDeveloperService projectDeveloperService;

    /**
     * Creates a VersionController.
     *
     * @param buildProperties         the build properties
     * @param projectDeveloperService the project developer service
     */
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

    /**
     * Reads the implementation version from the package metadata.
     */
    private static String packageVersion() {
        String version = VersionController.class.getPackage().getImplementationVersion();
        return version == null || version.isBlank() ? "development" : version;
    }

    /**
     * Returns the running application version.
     *
     * @return the running application version
     */
    @GetMapping(value = "/api/version", produces = MediaType.TEXT_PLAIN_VALUE)
    public String version() {
        return version;
    }

    /**
     * Returns the project developers loaded from build metadata.
     *
     * @return the project developers loaded from build metadata
     */
    @GetMapping(value = "/api/developers", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<ProjectDeveloperService.Developer> developers() {
        return projectDeveloperService.getDevelopers();
    }
}
