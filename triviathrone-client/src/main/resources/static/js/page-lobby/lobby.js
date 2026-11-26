import { apiFetch } from "../root/api.js";
import { getCurrentUser } from "../root/auth.js";
import { clearLobbyDetails, getLobbyDetails, getLobbyPassword } from "../root/lobby-storage.js";
import { handleDeltaMessage, handleHostUploadedGameMessage } from "./messages.js";
import * as socket from "../root/socket.js";

const currentUser = getCurrentUser();
const lobbyDetails = getLobbyDetails();
const lobbyCode = lobbyDetails.lobbyCode;

if (!lobbyCode) {
    console.warn("No lobby code found in Session");
    clearLobbyDetails();
    window.location.href = "/dashboard";
} else {
    document.getElementById("lobby-code").textContent = lobbyCode;
    initLobby().catch((error) => {
        console.error("Could not initialize the lobby.", error);
        clearLobbyDetails();
        window.alert(error.message || "Could not join this lobby.");
        window.location.href = "/dashboard";
    });
}

let lobbySubscription;
let privateGameSubscription;
let isLeaving = false;
let isStartingGame = false;

async function validateLobby(code) {
    const response = await apiFetch(`/api/lobbies/exists?code=${encodeURIComponent(code)}`);

    if (!response.ok) {
        throw new Error("Could not verify that this lobby exists.");
    }

    return response.json();
}

async function getLobbyData(code) {
    const response = await apiFetch(`/api/lobbies/info?code=${encodeURIComponent(code)}`);

    if (!response.ok) {
        throw new Error("Could not load lobby information.");
    }

    return response.json();
}

async function waitForMembership(code) {
    const deadline = Date.now() + 5000;

    while (Date.now() < deadline) {
        const lobbyData = await getLobbyData(code);

        if (lobbyData?.usernames?.includes(currentUser)) {
            return lobbyData;
        }

        await new Promise((resolve) => window.setTimeout(resolve, 200));
    }

    throw new Error("The lobby password was rejected or the lobby is unavailable.");
}

async function initLobby() {
    const valid = await validateLobby(lobbyCode);

    if (!valid) {
        console.warn("Lobby does not exist");
        window.location.href = "/dashboard";

        return;
    }

    await socket.connectSocket("/ws/lobby");

    lobbySubscription = socket.subscribe(`/topic/lobby.${lobbyCode}`, (data) => {
        if (data.action) {
            handleDeltaMessage(data);
        } else if (data.fileName) {
            handleHostUploadedGameMessage(data);
        } else {
            renderPlayers(data.usernames, data.lobbyHost);
        }
    });

    // Keep the latest recipient-specific state available to the mod-provided game page.
    privateGameSubscription = socket.subscribePrivateGameState((state) => {
        if (state?.recipient && state.recipient !== currentUser) {
            return;
        }

        sessionStorage.setItem(`game.private-state.${lobbyCode}`, JSON.stringify(state));
    });

    socket.send("/app/join", {
        name: currentUser,
        lobbyCode: lobbyCode,
        password: getLobbyPassword() || null,
    });

    const lobbyData = await waitForMembership(lobbyCode);

    renderPlayers(lobbyData.usernames, lobbyData.lobbyHost);
    removeHostElements(lobbyData.lobbyHost);
    configureGameStart(lobbyData);
}

function removeHostElements(hostUsername) {
    const isHost = currentUser === hostUsername;

    document.querySelectorAll("[data-admin]").forEach((ele) => {
        if (!isHost) {
            ele.remove();
        } else {
            ele.style.display = "";
        }
    });
}

function configureGameStart(lobbyData) {
    const startButton = document.getElementById("start-game-btn");
    const isHost = currentUser === lobbyData.lobbyHost;

    if (startButton && isHost) {
        startButton.disabled = !lobbyData.gameLoaded;
        startButton.textContent = lobbyData.gameStarted ? "Open Game" : "Start Game";
        startButton.title = lobbyData.gameLoaded ? "" : "Upload a game before starting.";
    }

    socket.subscribe(`/topic/game.${lobbyCode}`, (state) => {
        if (state?.lobbyCode === lobbyCode) {
            if (state.gameEnded) {
                isStartingGame = false;
                addFeedMessage("The host ended the game. You are back in the lobby.");

                return;
            }

            isStartingGame = true;
            window.location.href = `/game?code=${encodeURIComponent(lobbyCode)}`;
        }
    });

    if (lobbyData.gameStarted) {
        socket.send("/app/game/action", { lobbyCode, action: "SYNC" });
    }

    if (startButton && isHost) {
        startButton.addEventListener("click", () => {
            startButton.disabled = true;
            startButton.textContent = "Starting…";
            socket.send("/app/game/action", { lobbyCode, action: "START_GAME" });
        });
    }
}

export const crownChar = " 👑";
export const playerList = document.getElementById("player-list");

const eventFeed = document.getElementById("event-feed");

export function renderPlayers(players, hostUsername) {
    playerList.innerHTML = "";
    players.forEach((username) => {
        const li = document.createElement("li");

        li.textContent = username;

        if (username === currentUser) {
            li.classList.add("you");
        }

        if (username === hostUsername) {
            const crown = document.createElement("span");

            crown.textContent = crownChar;
            li.appendChild(crown);
        }

        playerList.appendChild(li);
    });
}

export function addFeedMessage(text) {
    const div = document.createElement("div");

    div.textContent = text;
    eventFeed.appendChild(div);
    eventFeed.scrollTop = eventFeed.scrollHeight;
}

document.getElementById("leave-btn").addEventListener("click", () => {
    isLeaving = true;
    socket.send("/app/leave", { lobbyCode });

    if (lobbySubscription) {
        socket.unsubscribe(`/topic/lobby.${lobbyCode}`);
    }

    if (privateGameSubscription) {
        socket.unsubscribe("/user/queue/game.private");
    }

    sessionStorage.removeItem(`game.private-state.${lobbyCode}`);
    socket.disconnect();
    clearLobbyDetails();
    window.location.href = "/dashboard";
});

window.addEventListener("beforeunload", () => {
    if (lobbyCode && !isLeaving && !isStartingGame) {
        try {
            socket.send("/app/leave", { lobbyCode });
        } catch (error) {
            console.warn("Could not notify the lobby about leaving.", error);
        }

        socket.disconnect();
    }
});

const uploadBtn = document.getElementById("upload-btn");
const fileInput = document.getElementById("game-file");
const uploadStatus = document.getElementById("upload-status");

uploadBtn.addEventListener("click", async () => {
    uploadBtn.disabled = true;

    const file = fileInput.files[0];

    if (!file) {
        uploadStatus.textContent = "Please select a .game.throne file";
        uploadBtn.disabled = false;

        return;
    }

    if (!file.name.endsWith(".game.throne")) {
        uploadStatus.textContent = "Invalid file type.";
        uploadBtn.disabled = false;

        return;
    }

    const formData = new FormData();

    formData.append("file", file);
    formData.append("lobbyCode", lobbyCode);

    uploadStatus.textContent = "Uploading...";

    try {
        const res = await apiFetch("/api/game/upload", {
            method: "POST",
            body: formData,
        });

        const text = await res.text();

        if (!res.ok) {
            uploadStatus.textContent = "Upload failed: " + text;
            uploadBtn.disabled = false;

            return;
        }

        uploadStatus.textContent = "Upload successful.";

        const startButton = document.getElementById("start-game-btn");

        if (startButton) {
            startButton.disabled = false;
        }
    } catch (err) {
        console.error(err);
        uploadStatus.textContent = "Upload error.";
    }

    uploadBtn.disabled = false;
});
