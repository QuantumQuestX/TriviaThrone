const LOBBY_DETAILS_KEY = "lobbyDetails";
const LOBBY_PASSWORD_KEY = "lobbyPassword";

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
    sessionStorage.removeItem(LOBBY_PASSWORD_KEY);
}

export function saveLobbyPassword(password) {
    if (password) {
        sessionStorage.setItem(LOBBY_PASSWORD_KEY, password);
    } else {
        sessionStorage.removeItem(LOBBY_PASSWORD_KEY);
    }
}

export function getLobbyPassword() {
    return sessionStorage.getItem(LOBBY_PASSWORD_KEY) ?? "";
}
