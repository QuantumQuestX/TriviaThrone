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

document.getElementById("dashboard-btn").addEventListener("click", () => {
    dashboardSection.style.display = "block";
    accountSection.style.display = "none";
});

document.getElementById("account-btn").addEventListener("click", () => {
    dashboardSection.style.display = "none";
    accountSection.style.display = "block";
});

document.getElementById("back-dashboard-btn").addEventListener("click", () => {
    dashboardSection.style.display = "block";
    accountSection.style.display = "none";
});

document.getElementById("logout-btn").addEventListener("click", () => {
    localStorage.removeItem("token");
    window.location.href = "/login.html";
});

const lobbyList = document.getElementById("lobby-list");

async function updateLobbyList() {
    try {
        const res = await fetch("/api/lobbies", {
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Failed to fetch lobbies");

        const lobbies = await res.json();

        lobbyList.innerHTML = "";
        lobbies.forEach(lobby => {
            const li = document.createElement("li");
            li.textContent = `${lobby.name} (Code: ${lobby.code})`;
            lobbyList.appendChild(li);
        });
    } catch (err) {
        console.error(err);
        lobbyList.innerHTML = "<li>Error loading lobbies</li>";
    }
}

updateLobbyList();
setInterval(updateLobbyList, 3000);

document.getElementById("join-btn").addEventListener("click", async () => {
    const code = document.getElementById("join-code").value;
    if (!code) return alert("Enter a lobby code");

    try {
        const res = await fetch(`/api/lobbies/join?code=${encodeURIComponent(code)}`, {
            method: "POST",
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Failed to join lobby");

        const lobby = await res.json();
        alert(`Joined lobby "${lobby.name}"`);
        await updateLobbyList();
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
        alert(`Create lobby "${lobby.name}" (Code: ${lobby.code})`);
        await updateLobbyList();
    } catch (err) {
        console.error(err);
        alert("Error creating lobby: " + err.message);
    }
});