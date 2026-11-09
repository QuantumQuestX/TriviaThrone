package de.quantumquest.triviathrone.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VersionControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BuildProperties buildProperties = mock(BuildProperties.class);
        when(buildProperties.getVersion()).thenReturn("1.2.3");
        mockMvc = MockMvcBuilders.standaloneSetup(
                new VersionController(buildProperties)
        ).build();
    }

    @Test
    void returnsTheCurrentBuildVersionAsPlainText() throws Exception {
        mockMvc.perform(get("/api/version"))
                .andExpect(status().isOk())
                .andExpect(content().string("1.2.3"));
    }
}
