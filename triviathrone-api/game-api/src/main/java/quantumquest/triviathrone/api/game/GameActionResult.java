package quantumquest.triviathrone.api.game;

public record GameActionResult(boolean accepted, boolean gameEnded) {
    public static GameActionResult rejected() {
        return new GameActionResult(false, false);
    }

    public static GameActionResult allow() {
        return new GameActionResult(true, false);
    }

    public static GameActionResult endGame() {
        return new GameActionResult(true, true);
    }
}
