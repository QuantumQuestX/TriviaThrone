package de.quantumquest.triviathrone.game;

import de.quantumquest.triviathrone.game.definition.Category;
import de.quantumquest.triviathrone.game.definition.MediaType;
import de.quantumquest.triviathrone.game.definition.QuestionDefinition;
import de.quantumquest.triviathrone.game.definition.Round;
import de.quantumquest.triviathrone.server.Lobby;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class GameSessionTest {
    private GameSession session;

    @AfterEach
    void closeSession() {
        if (session != null) session.close();
    }

    @Test
    void startingVideoOpensBuzzerAndPublishesMediaUrl() {
        session = createSession(MediaType.VIDEO, "resources/clip one.mp4");
        session.selectQuestion(0, 0, 0);

        session.playMedia();

        assertEquals(GameSession.State.BUZZ_OPEN, session.getCurrentState());
        assertNotNull(session.getMediaPlaybackToken());
        assertEquals(
                "/api/game/ROOM01/media?path=resources%2Fclip+one.mp4",
                session.getCurrentQuestionMessage().mediaUrl()
        );
        assertNull(session.getMediaPauseToken());
    }

    @Test
    void firstBuzzEmitsPauseTokenAndResetClearsPlaybackState() {
        session = createSession(MediaType.AUDIO, "resources/clue.mp3");
        session.selectQuestion(0, 0, 0);
        session.playMedia();
        String playbackToken = session.getMediaPlaybackToken();

        session.registerBuzz("player");
        session.registerBuzz("another-player");
        session.closeBuzz();

        assertEquals(GameSession.State.BUZZ_LOCKED, session.getCurrentState());
        assertNotNull(session.getMediaPauseToken());
        assertNotNull(playbackToken);

        session.resetToIdle();

        assertEquals(GameSession.State.IDLE, session.getCurrentState());
        assertNull(session.getMediaPlaybackToken());
        assertNull(session.getMediaPauseToken());
    }

    @Test
    void nonAudioVideoMediaDoesNotOpenBuzzer() {
        session = createSession(MediaType.IMAGE, "resources/clue.png");
        session.selectQuestion(0, 0, 0);

        session.playMedia();

        assertEquals(GameSession.State.QUESTION_SELECTED, session.getCurrentState());
        assertNull(session.getMediaPlaybackToken());
    }

    @Test
    void mediaCanStartAfterHostManuallyOpensBuzzer() {
        session = createSession(MediaType.VIDEO, "resources/clue.mp4");
        session.selectQuestion(0, 0, 0);
        session.openBuzz();

        session.playMedia();

        assertEquals(GameSession.State.BUZZ_OPEN, session.getCurrentState());
        assertNotNull(session.getMediaPlaybackToken());
    }

    private static GameSession createSession(MediaType mediaType, String mediaPath) {
        Lobby lobby = new Lobby("ROOM01", "Test lobby", "host");
        lobby.addUser("player");
        QuestionDefinition question = new QuestionDefinition(100, "Prompt", mediaPath, mediaType);
        GameDefinition definition = new GameDefinition(
                "Test game",
                "Description",
                List.of(new Round("Round 1", List.of(
                        new Category("Category", List.of(question))
                )))
        );
        return new GameSession(lobby, definition);
    }
}
