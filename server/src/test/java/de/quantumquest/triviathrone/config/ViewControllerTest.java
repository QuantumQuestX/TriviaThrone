package de.quantumquest.triviathrone.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ViewControllerTest {
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ViewController()).build();
    }

    @Test
    void cleanPageRoutesForwardToStaticHtmlWithoutRedirecting() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/index.html"));
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/login.html"));
        mockMvc.perform(get("/lobby"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/lobby.html"));
        mockMvc.perform(get("/game-creator"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("/game-creator.html"));
        mockMvc.perform(get("/account"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/?page=account"));
    }
}
