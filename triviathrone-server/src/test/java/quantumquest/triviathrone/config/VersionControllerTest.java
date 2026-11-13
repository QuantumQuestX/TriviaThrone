package quantumquest.triviathrone.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VersionControllerTest {
    private MockMvc mockMvc;
    private ProjectDeveloperService developers;

    @BeforeEach
    void setUp() {
        BuildProperties buildProperties = mock(BuildProperties.class);
        when(buildProperties.getVersion()).thenReturn("1.2.3");
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("buildProperties", buildProperties);
        ObjectProvider<BuildProperties> buildPropertiesProvider = beans.getBeanProvider(BuildProperties.class);
        developers = mock(ProjectDeveloperService.class);
        when(developers.getDevelopers()).thenReturn(List.of(
                new ProjectDeveloperService.Developer(
                        "TiJ",
                        "https://github.com/TiJ-code",
                        List.of("MAINTAINER"),
                        List.of(new ProjectDeveloperService.Organization(
                                "QuantumQuest",
                                "https://github.com/QuantumQuestX"
                        ))
                )
        ));
        mockMvc = MockMvcBuilders.standaloneSetup(
                new VersionController(buildPropertiesProvider, developers)
        ).build();
    }

    @Test
    void returnsTheCurrentBuildVersionAsPlainText() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(status().isOk())
                .andExpect(content().string("1.2.3"));
    }

    @Test
    void returnsDevelopmentVersionWhenBuildInfoIsUnavailable() throws Exception {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        mockMvc = MockMvcBuilders.standaloneSetup(
                new VersionController(beans.getBeanProvider(BuildProperties.class), developers)
        ).build();

        mockMvc.perform(get("/api/version"))
                .andExpect(status().isOk())
                .andExpect(content().string("development"));
    }

    @Test
    void returnsDeveloperMetadataAsJson() throws Exception {
        mockMvc.perform(get("/api/developers"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{
                          "name": "TiJ",
                          "url": "https://github.com/TiJ-code",
                          "roles": ["MAINTAINER"],
                          "organizations": [{
                            "name": "QuantumQuest",
                            "url": "https://github.com/QuantumQuestX"
                          }]
                        }]
                        """));
        verify(developers).getDevelopers();
    }
}
