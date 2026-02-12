const token = localStorage.getItem("token");

function parseJwt(token) {
    try {
        const payload = token.split(".")[1];
        return JSON.parse(atob(payload));
    } catch {
        return null;
    }
}

const currentUser = parseJwt(token)?.sub || null;

const playerList = document.getElementById("player-list");
const eventFeed = document.getElementById("event-feed");

function addPlayer(username, isHost = false) {
    const li = document.createElement("li");
    li.textContent = username;
    li.classList.add("joined");

    if (currentUser !== null && username === currentUser) {
        li.classList.add("you");

        const youTag = document.createElement("span");
        youTag.textContent = " (You)";
        youTag.style.color = "#6c5ce7";
        youTag.style.fontWeight = "normal";
        li.appendChild(youTag);
    }

    if (isHost) {
        const crown = document.createElement("span");
        crown.textContent = " 👑";
        li.appendChild(crown);
    }

    playerList.appendChild(li);
    addFeedMessage(username + " joined the lobby");
}

function removePlayer(username) {
    const players = playerList.querySelectorAll("li");
    players.forEach(li => {
        if (li.textContent.includes(username)) {
            li.classList.remove("joined");
            li.classList.add("left");
            setTimeout(() => li.remove(), 800);
        }
    });

    addFeedMessage(username + " left the lobby");
}

function addFeedMessage(text) {
    const div = document.createElement("div");
    div.textContent = text;
    eventFeed.appendChild(div);
    eventFeed.scrollTop = eventFeed.scrollHeight;
}

addPlayer("HostUser", true);
addPlayer("Alice");
addPlayer("Bob");

setTimeout(() => removePlayer("Bob"), 5000);

document.getElementById("upload-btn").addEventListener("click", () => {
    const file = document.getElementById("game-file").files[0];
    const status = document.getElementById("upload-status");

    if (!file) {
        status.textContent = "Please select a file.";
        return;
    }

    status.textContent = "Uploading " + file.name + "...";
});