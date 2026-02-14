import { token, currentUser } from "../root/token.js";
import { handleDeltaMessage } from "./messages.js";

const lobbyDetails = JSON.parse(localStorage.getItem("lobbyDetails")) || "{}";
const lobbyCode = lobbyDetails.lobbyCode;

if (!lobbyCode || !(await validateLobby(lobbyCode))) {
    console.warn("No lobby code found in Session");
    localStorage.removeItem("lobbyDetails");
    window.location.href = "/";
}
document.getElementById("lobby-code").textContent = lobbyCode;

let stompClient, lobbySubscription;

async function validateLobby(code) {
    try {
        const res = await fetch(`/api/lobbies?code=${encodeURIComponent(code)}`, {
            method: "POST",
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Validation request failed");

        return await res.json();
    } catch (err) {
        console.error("Validation error: ", err);
        return false;
    }
}

async function getLobbyData(code) {
    try {
        const res = await fetch(`/api/lobbies/info?code=${encodeURIComponent(code)}`, {
            method: "GET",
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Could not get info");

        return await res.json();
    } catch (err) {
        console.error("Could not get info");
        return null;
    }
}

async function initLobby() {
    const valid = await validateLobby(lobbyCode);

    if (!valid) {
        console.warn("Lobby does not exist");
        window.location.href = "/";
        return;
    }

    const socket = new SockJS("/ws/lobby");
    stompClient = Stomp.over(socket);

    stompClient.connect(
        { "Authorization": "Bearer " + token },
        () => {
            console.log("Connected to lobby: ", lobbyCode);

            lobbySubscription = stompClient.subscribe(`/topic/lobby.${lobbyCode}`, (message) => {
                const data = JSON.parse(message.body);

                if (data.action) {
                    handleDeltaMessage(data);
                } else {
                    renderPlayers(data.usernames, data.lobbyHost);
                }
            });
            stompClient.send("/app/join", {}, JSON.stringify( { lobbyCode: lobbyCode, name: null } ));
        }
    );

    const lobbyData = await getLobbyData(lobbyDetails.lobbyCode);
    console.log("lobbyData", lobbyData);
    renderPlayers(lobbyData.usernames, lobbyData.lobbyHost);
}

initLobby();

export const crownChar = " 👑";
export const playerList = document.getElementById("player-list");
const eventFeed = document.getElementById("event-feed");

export function renderPlayers(players, hostUsername) {
    playerList.innerHTML = "";
    players.forEach(username => {
        const li = document.createElement("li");
        li.textContent = username;

        console.log(username, hostUsername, currentUser, username === hostUsername);

        if (username === currentUser) li.classList.add("you");
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
    stompClient.send("/app/leave", {}, JSON.stringify( { lobbyCode: lobbyCode, name: null } ));

    if (lobbySubscription) {
        lobbySubscription.unsubscribe();
    }
    
    stompClient.disconnect(() => {
        console.log("Disconnected cleanly");
        window.location.href = "/index.html";
    });
});

window.addEventListener("beforeunload", () => {
    if (stompClient && stompClient.connected) {
        stompClient.send("/app/leave", {}, JSON.stringify( { lobbyCode: lobbyCode, name: null } ));
        stompClient.disconnect();
    }
});
