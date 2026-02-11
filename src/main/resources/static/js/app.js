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

function updateLobbyList() {
    const lobbies = [
        { name: "LAN Night Quiz", code: 'B456' }
    ];

    lobbyList.innerHTML = "";
    lobbies.forEach(lobby => {
        const li = document.createElement("li");
        li.textContent = `${lobby.name} (Code: ${lobby.code})`;
        lobbyList.appendChild(li);
    });
}

updateLobbyList();

document.getElementById("join-btn").addEventListener("click", () => {
    const code = document.getElementById("join-code").value;
    alert("Join lobby " + code + " (implement API call here)");
});

document.getElementById("create-btn").addEventListener("click", () => {
    const name = document.getElementById("lobby-name").value;
    alert("Create lobby: " + name + " (implement API call here)");
});