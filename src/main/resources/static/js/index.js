console.log(token);
if (!token) {
    window.location.href = "/login.html";
}
initTopbarUserByToken(parseJwt(token));

const dashboardSection = document.getElementById("dashboard-section");
const accountSection = document.getElementById("account-section");

function setSectionVisible(section, visible) {
    section.style.display = visible ? "block" : "none";
}

// Topbar button
document.getElementById("dashboard-btn").addEventListener("click", () => {
    setSectionVisible(dashboardSection, true);
    setSectionVisible(accountSection, false);
});

document.getElementById("account-btn").addEventListener("click", () => {
    setSectionVisible(dashboardSection, false);
    setSectionVisible(accountSection, true);
});

// Account page via URL param
const params = new URLSearchParams(window.location.search);
const page = params.get("page");

if (page === "account") {
    setSectionVisible(dashboardSection, false);
    setSectionVisible(accountSection, true);
}

// Lobby browser
let selectedLobby = null;
const lobbyList = document.getElementById("lobby-list");

async function loadLobbies() {
    try {
        const res = await fetch("/api/lobbies", {
            method: "GET",
            headers: { "Authorization": "Bearer " + token }
        });

        if (!res.ok) throw new Error("Failed to fetch lobbies");

        const lobbies = await res.json();
        lobbyList.innerHTML = "";
        selectedLobby = null;

        lobbies.forEach(lobby => {
            const li = document.createElement("li");
            li.textContent = `${lobby.name} (Code: ${lobby.code})`;
            li.style.cursor = "pointer";

            if (selectedLobby === lobby.code) {
                li.classList.add("selected");
            }

            li.addEventListener("click", () => {
                document.querySelectorAll("#lobby-list li").forEach(n => n.classList.remove("selected"));
                li.classList.add("selected");
                selectedLobby = lobby.code;
            })

            lobbyList.appendChild(li);
        });
    } catch (err) {
        console.error(err);
        lobbyList.innerHTML = "<li>Error loading lobbies</li>";
    }
}

// Refresh regulary (3s)
loadLobbies();
setInterval(loadLobbies, 3000);

document.getElementById("join-btn").addEventListener("click", async () => {
    const code = document.getElementById("join-code").value;
    if (!code) return alert("Enter a lobby code");

    localStorage.setItem("lobbyCode", code);
    window.location.href = `/lobby.html`;
});

document.getElementById("logout-btn").addEventListener("click", () => {
    localStorage.removeItem("token");
    window.location.href = "/login.html";
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