import { token } from "../root/token.js";

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
            li.textContent = `${lobby.name} (Code: ${lobby.lobbyCode})`;
            li.style.cursor = "pointer";

            if (selectedLobby === lobby.lobbyCode) {
                li.classList.add("selected");
            }

            li.addEventListener("click", () => {
                document.querySelectorAll("#lobby-list li").forEach(n => n.classList.remove("selected"));
                li.classList.add("selected");
                selectedLobby = lobby.lobbyCode;
            })

            lobbyList.appendChild(li);
        });
    } catch (err) {
        console.error(err);
        lobbyList.innerHTML = "<li>Error loading lobbies</li>";
    }
}

loadLobbies();
setInterval(loadLobbies, 3000);

// Join lobby
document.getElementById("join-btn").addEventListener("click", async () => {
    const code = selectedLobby || document.getElementById("join-code").value;
    if (!code) return alert("Enter a lobby code");

    localStorage.setItem("lobbyCode", code);
    window.location.href = `/lobby.html?code=${code}`;
});

// Logout
document.getElementById("logout-btn").addEventListener("click", () => {
    localStorage.removeItem("token");
    window.location.href = "/login.html";
});

// Create lobby
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

        window.location.href = `/lobby.html?code=${encodeURIComponent(lobby.lobbyCode)}`;
    } catch (err) {
        console.error(err);
        alert("Error creating lobby: " + err.message);
    }
});