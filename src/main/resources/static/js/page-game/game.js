import * as socket from "../root/socket.js";

const Actions = Object.freeze({
    SELECT_QUESTION: "SELECT_QUESTION",
    OPEN_BUZZ: "OPEN_BUZZ",
    BUZZ: "BUZZ",
    MARK_CORRECT: "MARK_CORRECT",
    MARK_INCORRECT: "MARK_INCORRECT",
    RESET: "RESET"
});

let currentTopic = null;

export async function subscribeToGame(lobbyCode, onStateUpdate) {
    await socket.connectSocket("/ws/game");
    
    const topic = `/topic/game.${lobbyCode}`;

    if (currentTopic) {
        socket.unsubscribe(currentTopic);
    }

    socket.subscribe(topic, (state) => {
        onStateUpdate(state);
    });

    currentTopic = topic;
}

export async function sendOpenBuzz(lobbyCode) {
    sendAction(lobbyCode, Actions.OPEN_BUZZ);
}

export async function sendBuzz(lobbyCode) {
    sendAction(lobbyCode, Actions.BUZZ);
}

export async function setMarkCorrect(lobbyCode) {
    sendAction(lobbyCode, Actions.MARK_CORRECT);
}

export async function setMarkIncorrect(lobbyCode) {
    sendAction(lobbyCode, Actions.MARK_INCORRECT);
}

export async function sendReset(lobbyCode) {
    sendAction(lobbyCode, Actions.RESET);
}

function sendAction(lobbyCode, action) {
    socket.send("/app/game/action", {
        lobbyCode: lobbyCode,
        action: action
    });
}