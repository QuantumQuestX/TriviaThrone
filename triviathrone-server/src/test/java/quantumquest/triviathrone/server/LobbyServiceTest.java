package quantumquest.triviathrone.server;

import quantumquest.triviathrone.api.system.events.ISystemEvent;
import quantumquest.triviathrone.api.system.events.LobbyClosedEvent;
import quantumquest.triviathrone.api.system.events.LobbyCreatedEvent;
import quantumquest.triviathrone.auth.UserAccount;
import quantumquest.triviathrone.mod.SystemModManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LobbyServiceTest {
    @Test
    void publishesLobbyCreationAndClosureEvents() {
        SystemModManager systemMods = mock(SystemModManager.class);
        UserAccount host = new UserAccount();
        host.setUsername("host");
        LobbyService lobbyService = new LobbyService(systemMods);

        Lobby lobby = lobbyService.createLobby("Test lobby", host);
        lobbyService.removeLobby(lobby.getLobbyCode());

        ArgumentCaptor<ISystemEvent> events = ArgumentCaptor.forClass(ISystemEvent.class);
        verify(systemMods, times(2)).publishEvent(events.capture());
        LobbyCreatedEvent created = assertInstanceOf(LobbyCreatedEvent.class, events.getAllValues().get(0));
        LobbyClosedEvent closed = assertInstanceOf(LobbyClosedEvent.class, events.getAllValues().get(1));
        assertEquals(lobby.getLobbyCode(), created.lobbyCode());
        assertEquals("host", created.hostUsername());
        assertEquals(lobby.getLobbyCode(), closed.lobbyCode());
    }

    @Test
    void supportsPasswordProtectedLobbiesWithoutExposingThePassword() {
        SystemModManager systemMods = mock(SystemModManager.class);
        UserAccount host = new UserAccount();
        host.setUsername("host");
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        LobbyService lobbyService = new LobbyService(systemMods, encoder);

        Lobby lobby = lobbyService.createLobby("Private lobby", host, "StrongPassword1!");

        assertTrue(lobby.isPasswordProtected());
        assertTrue(lobby.acceptsPassword("StrongPassword1!", encoder));
        assertFalse(lobby.acceptsPassword("WrongPassword1!", encoder));
        assertFalse(lobby.toString().contains("StrongPassword1!"));
        assertFalse(lobby.toString().contains("$2"));
    }

    @Test
    void legacyLobbyCreationRemainsOpen() {
        SystemModManager systemMods = mock(SystemModManager.class);
        UserAccount host = new UserAccount();
        host.setUsername("host");

        Lobby lobby = new LobbyService(systemMods).createLobby("Open lobby", host);

        assertFalse(lobby.isPasswordProtected());
        assertTrue(lobby.acceptsPassword(null, new BCryptPasswordEncoder()));
    }
}
