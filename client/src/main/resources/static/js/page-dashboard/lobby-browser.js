import {apiFetch} from "../root/api.js";
import {saveLobbyDetails} from "../root/lobby-storage.js";

const lobbyList = document.getElementById("lobby-list");
const joinCodeInput = document.getElementById("join-code");
let selectedLobbyCode = null;
let isLoadingLobbies = false;

async function loadLobbies() {
    if (isLoadingLobbies) return;
    isLoadingLobbies = true;

    try {
        const response = await apiFetch("/api/lobbies");
        if (!response.ok) {
            throw new Error("Could not load available lobbies.");
        }

        const lobbies = await response.json();
        const selectedCode = selectedLobbyCode;
        const fragment = document.createDocumentFragment();

        if (lobbies.length === 0) {
            const emptyMessage = document.createElement("li");
            emptyMessage.setAttribute("role", "status");
            emptyMessage.textContent = "No lobbies available.";
            fragment.appendChild(emptyMessage);
        }

        lobbies.forEach(lobby => {
            const item = document.createElement("li");
            item.textContent = `${lobby.name} (Code: ${lobby.lobbyCode})`;
            item.classList.toggle("selected", lobby.lobbyCode === selectedCode);
            item.addEventListener("click", () => {
                selectedLobbyCode = lobby.lobbyCode;
                joinCodeInput.value = lobby.lobbyCode;
                lobbyList.querySelector(".selected")?.classList.remove("selected");
                item.classList.add("selected");
            });
            fragment.appendChild(item);
        });

        lobbyList.replaceChildren(fragment);
        if (!lobbies.some(lobby => lobby.lobbyCode === selectedCode)) {
            selectedLobbyCode = null;
        }
    } catch (error) {
        lobbyList.replaceChildren();
        const item = document.createElement("li");
        item.setAttribute("role", "status");
        item.textContent = error.message || "Error loading lobbies.";
        lobbyList.appendChild(item);
    } finally {
        isLoadingLobbies = false;
    }
}

async function joinLobby() {
    const lobbyCode = (selectedLobbyCode || joinCodeInput.value).trim();
    if (!lobbyCode) {
        window.alert("Enter a lobby code.");
        return;
    }

    try {
        const response = await apiFetch(`/api/lobbies/exists?code=${encodeURIComponent(lobbyCode)}`);
        if (!response.ok || !(await response.json())) {
            throw new Error("That lobby does not exist.");
        }

        saveLobbyDetails({lobbyCode});
        window.location.assign("/lobby");
    } catch (error) {
        window.alert(error.message);
    }
}

async function createLobby() {
    const name = document.getElementById("lobby-name").value.trim();
    if (!name) {
        window.alert("Enter a lobby name.");
        return;
    }

    try {
        const parameters = new URLSearchParams({name});
        const response = await apiFetch(`/api/lobbies/create?${parameters}`, {method: "POST"});
        if (!response.ok) {
            throw new Error("Could not create the lobby.");
        }

        saveLobbyDetails(await response.json());
        window.location.assign("/lobby");
    } catch (error) {
        window.alert(`Error creating lobby: ${error.message}`);
    }
}

document.getElementById("join-btn").addEventListener("click", joinLobby);
document.getElementById("create-btn").addEventListener("click", createLobby);
void loadLobbies();
const lobbyRefreshTimer = window.setInterval(loadLobbies, 3000);
window.addEventListener("pagehide", () => window.clearInterval(lobbyRefreshTimer), {once: true});
