import { getCurrentUser } from "../root/auth.js";
import { playerList, crownChar, addFeedMessage } from "./lobby.js";

const currentUser = getCurrentUser();

export function handleDeltaMessage(deltaMsg) {
    const { lobbyHost, username, action } = deltaMsg;

    if (!lobbyHost || !username || !action) {
        return;
    }

    const existing = Array.from(playerList.children).find((li) => li.textContent.replace(crownChar, "") === username);

    const isSelf = username === currentUser;

    if (action === "JOIN") {
        if (!existing) {
            const li = document.createElement("li");

            li.textContent = username;

            if (isSelf) {
                li.classList.add("you");
            }

            if (username === lobbyHost) {
                const crown = document.createElement("span");

                crown.textContent = crownChar;
                li.appendChild(crown);
            }

            playerList.appendChild(li);

            if (isSelf) {
                addFeedMessage("You joined the lobby");
            } else {
                addFeedMessage(`${username} joined the lobby`);
            }
        }
    } else if (action === "LEAVE") {
        if (existing) {
            playerList.removeChild(existing);

            if (isSelf) {
                addFeedMessage("You left the lobby");
            } else {
                addFeedMessage(`${username} left the lobby`);
            }
        }
    }
}

export function handleHostUploadedGameMessage(uploadMsg) {
    const { lobbyCode, hostName, fileName } = uploadMsg;

    if (!lobbyCode || !hostName || !fileName) {
        return;
    }

    if (hostName === currentUser) {
        addFeedMessage("You uploaded a game.");
    } else {
        addFeedMessage("Host " + hostName + ' uploaded game "' + fileName + '".');
    }
}
