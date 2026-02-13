const token = localStorage.getItem("token");
if (!token) {
    window.location.href = "/login.html";
}

function parseJwt(token) {
    try {
        const payload = token.split(".")[1];
        return JSON.parse(atob(payload));
    } catch (e) {
        return null;
    }
}

const payload = parseJwt(token);
const username = payload?.sub || "Unknown";

document.getElementById("username-display").textContent = username;
document.getElementById("token-expiry").textContent = payload?.exp
                        ? new Date(payload.exp * 1000).toLocaleString()
                        : "Unkown";

const dashboardSection = document.getElementById("dashboard-section");
const accountSection = document.getElementById("account-section");

function setSectionVisible(section, visible) {
    section.style.display = visible ? "block" : "none";
}

document.getElementById("dashboard-btn").addEventListener("click", () => {
    setSectionVisible(dashboardSection, true);
    setSectionVisible(accountSection, false);
});

document.getElementById("account-btn").addEventListener("click", () => {
    setSectionVisible(dashboardSection, false);
    setSectionVisible(accountSection, true);
});

document.getElementById("logout-btn").addEventListener("click", () => {
    localStorage.removeItem("token");
    window.location.href = "/login.html";
});

const params = new URLSearchParams(window.location.search);
const page = params.get("page");

if (page == "account") {
    setSectionVisible(dashboardSection, false);
    setSectionVisible(accountSection, true);
}

let selectedLobby = null;
let selectedLobbyLi = null;
const lobbyList = document.getElementById("lobby-list");

async function updateLobbyList() {
    try {
        const res = await fetch("/api/lobbies", {
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Failed to fetch lobbies");

        const lobbies = await res.json();

        lobbyList.innerHTML = "";
        selectedLobbyLi = null;

        lobbies.forEach(lobby => {
            const li = document.createElement("li");
            li.textContent = `${lobby.name} (Code: ${lobby.code})`;
            li.style.cursor = "pointer";

            if (selectedLobby === lobby.code) {
                li.classList.add("selected");
                selectedLobbyLi = li;
            }

            li.addEventListener("click", () => {
                if (selectedLobbyLi) {
                    selectedLobbyLi.classList.remove("selected");
                }

                selectedLobby = lobby.code;
                selectedLobbyLi = li;

                li.classList.add("selected");
            })

            lobbyList.appendChild(li);
        });

        const stillExists = lobbies.some(l => l.code === selectedLobby);
        if (!stillExists) {
            selectedLobby = null;
            selectedLobbyLi = null;
        }
    } catch (err) {
        console.error(err);
        lobbyList.innerHTML = "<li>Error loading lobbies</li>";
    }
}

updateLobbyList();
setInterval(updateLobbyList, 3000);

document.getElementById("join-btn").addEventListener("click", async () => {
    const code = selectedLobby || document.getElementById("join-code").value;
    if (!code) return alert("Enter a lobby code");

    try {
        const res = await fetch(`/api/lobbies/join?code=${encodeURIComponent(code)}`, {
            method: "POST",
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Failed to join lobby");

        const lobby = await res.json();
        window.location.href = `/lobby.html?code=${encodeURI(lobby.code)}`;
    } catch (err) {
        console.error(err);
        alert("Error joining lobby: " + err.message);
    }
});

document.getElementById("create-btn").addEventListener("click", async () => {
    const name = document.getElementById("lobby-name").value;
    if (!name) return alert("Enter a lobby name");

    try {
        const res = await fetch(`/api/lobbies/create?name=${encodeURIComponent(name)}`, {
            method: "POST",
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Failed to create lobby");

        const lobby = await res.json();
        window.location.href = `/lobby.html?code=${encodeURIComponent(lobby.code)}`;
    } catch (err) {
        console.error(err);
        alert("Error creating lobby: " + err.message);
    }
});