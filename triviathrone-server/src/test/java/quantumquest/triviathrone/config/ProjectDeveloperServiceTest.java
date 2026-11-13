package quantumquest.triviathrone.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectDeveloperServiceTest {
    @Test
    void readsDeveloperRolesAndOrganizationsFromMavenPom() {
        ProjectDeveloperService service = new ProjectDeveloperService();

        assertEquals(2, service.getDevelopers().size());
        ProjectDeveloperService.Developer maintainer = service.getDevelopers().getFirst();
        assertEquals("TiJ", maintainer.name());
        assertEquals("https://github.com/TiJ-code", maintainer.url());
        assertTrue(maintainer.roles().contains("MAINTAINER"));
        assertEquals(
                new ProjectDeveloperService.Organization("QuantumQuest", "https://github.com/QuantumQuestX"),
                maintainer.organizations().getFirst()
        );
    }
}
