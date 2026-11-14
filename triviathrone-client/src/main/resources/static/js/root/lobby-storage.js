const LOBBY_DETAILS_KEY = "lobbyDetails";

export function getLobbyDetails() {
    try {
        const details = JSON.parse(localStorage.getItem(LOBBY_DETAILS_KEY) ?? "null");

        if (
            details &&
            typeof details === "object" &&
            !Array.isArray(details) &&
            typeof details.lobbyCode === "string" &&
            details.lobbyCode.trim()
        ) {
            return { ...details, lobbyCode: details.lobbyCode.trim() };
        }
    } catch (error) {
        console.warn("Ignoring invalid saved lobby details.", error);
    }

    clearLobbyDetails();

    return {};
}

export function saveLobbyDetails(details) {
    localStorage.setItem(LOBBY_DETAILS_KEY, JSON.stringify(details));
}

export function clearLobbyDetails() {
    localStorage.removeItem(LOBBY_DETAILS_KEY);
}
