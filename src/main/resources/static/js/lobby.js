initTopbarUserByToken(parseJwt(token));

const lobbyCode = new URLSearchParams(window.location.search).get("code");
if (!lobbyCode) {
    console.warn("No lobby code found in Session");
    window.location.href = "/";
}

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

async function initLobby() {
    const valid = await validateLobby(lobbyCode);

    if (!valid) {
        console.warn("Lobby does not exist");
        window.location.href = "/";
        return;
    }

    console.log("is valid");

    document.getElementById("lobby-code").textContent = lobbyCode;

    const socket = new SockJS("/ws/lobby");
    stompClient = Stomp.over(socket);

    stompClient.connect(
        { "Authorization": "Bearer " + token },
        () => {
            console.log("Connected to lobby: ", lobbyCode);

            lobbySubscription = stompClient.subscribe(`/topic/lobby.${lobbyCode}`, (message) => {
                const data = JSON.parse(message.body);
                renderPlayers(data.usernames);
                addFeedMessage("Lobby updated");
            });
        }
    );
}

initLobby();

const playerList = document.getElementById("player-list");
const eventFeed = document.getElementById("event-feed");

function renderPlayers(players, hostUsername) {
    playerList.innerHTML = "";
    players.forEach(username => {
        const li = document.createElement("li");
        li.textContent = username;

        if (username === currentUser) li.classList.add("you");
        if (username === hostUsername) {
            const crown = document.createElement("span");
            crown.textContent = " 👑";
            li.appendChild(crown);
        }

        playerList.appendChild(li);
    });
}

function addFeedMessage(text) {
    const div = document.createElement("div");
    div.textContent = text;
    eventFeed.appendChild(div);
    eventFeed.scrollTop = eventFeed.scrollHeight;
}

document.getElementById("leave-btn").addEventListener("click", () => {
    stompClient.send("/app/leave", {}, JSON.stringify( { lobbyCode } ));

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
        stompClient.send("/app/leave", {}, JSON.stringify( { lobbyCode } ));
        stompClient.disconnect();
    }
});
