package quantumquest.triviathrone.server;

import quantumquest.triviathrone.api.system.events.ISystemEvent;
import quantumquest.triviathrone.api.system.events.LobbyClosedEvent;
import quantumquest.triviathrone.api.system.events.LobbyCreatedEvent;
import quantumquest.triviathrone.auth.UserAccount;
import quantumquest.triviathrone.mod.SystemModManager;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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
}
