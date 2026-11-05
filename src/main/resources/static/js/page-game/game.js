import * as gameSocket from "./game-socket.js";
import * as socket from "../root/socket.js";
import {PlayerView} from "./jeopardy/player-view.js";
import {apiFetch} from "../root/api.js";
import {getCurrentUser} from "../root/auth.js";
import {clearLobbyDetails, getLobbyDetails} from "../root/lobby-storage.js";
import {QuestionManager} from "./jeopardy/question-manager.js";

const currentUser = getCurrentUser();
const lobbyCode = new URLSearchParams(window.location.search).get("code")
    || getLobbyDetails().lobbyCode;
const playerViews = new Map();
const questionManager = new QuestionManager();
let isHost = false;
let hostUsername = null;
let lastQuestionKey = null;
let selectedRoundIdx = 0;

const stateLabels = {
    IDLE: "Choose a question to begin.",
    QUESTION_SELECTED: "Question selected. The host can open the buzzer.",
    QUESTION_OPEN: "Question is open.",
    BUZZ_OPEN: "Buzz in now!",
    BUZZ_COLLECTING: "Buzz received. Locking in the fastest player…",
    BUZZ_LOCKED: "Buzzer locked. The host is judging the answer.",
    ANSWER_REVEAL: "Answer marked correct."
};

if (!lobbyCode) {
    window.location.href = "/index.html";
} else {
    initializeGame().catch(error => {
        console.error("Could not start the game page:", error);
        document.getElementById("game-status").textContent = "Could not connect to this game. Return to the lobby and try again.";
    });
}

async function initializeGame() {
    const response = await apiFetch(`/api/lobbies/info?code=${encodeURIComponent(lobbyCode)}`);
    if (!response.ok) throw new Error("Could not load lobby information");

    const lobby = await response.json();
    if (!lobby) {
        localStorage.removeItem("lobbyDetails");
        window.location.href = "/index.html";
        return;
    }

    hostUsername = lobby.lobbyHost;
    isHost = hostUsername === currentUser;
    document.getElementById("lobby-code").textContent = lobbyCode;
    document.getElementById("username-display").textContent = currentUser || "Unknown";
    document.getElementById("host-panel").style.display = isHost ? "" : "none";
    document.getElementById("main-buzzer-container").classList.toggle("hidden", isHost);
    layoutPlayers([...lobby.usernames].sort());

    await gameSocket.subscribeToGame(lobbyCode, state => {
        if (state?.gameEnded) {
            window.location.href = "/lobby.html";
            return;
        }
        applyGameState(state);
    });
    gameSocket.sendAction(lobbyCode, gameSocket.Actions.SYNC);
    configureHostControls();
    configureLeaveButton();
}

function layoutPlayers(players) {
    const left = document.getElementById("players-left");
    const right = document.getElementById("players-right");
    const others = players.filter(username => username !== currentUser && username !== hostUsername);
    const midpoint = Math.ceil(others.length / 2);

    left.replaceChildren();
    right.replaceChildren();
    playerViews.clear();

    others.slice(0, midpoint).forEach(username => {
        const view = new PlayerView(username);
        view.mount(left);
        playerViews.set(username, view);
    });

    others.slice(midpoint).forEach(username => {
        const view = new PlayerView(username);
        view.mount(right);
        playerViews.set(username, view);
    });

    if (!isHost) {
        playerViews.set(currentUser, new PlayerView(currentUser, {isSelf: true}));
    }
}

function configureHostControls() {
    document.getElementById("host-buzzer-enable").addEventListener("click", () => {
        gameSocket.sendOpenBuzz(lobbyCode);
    });
    document.getElementById("host-buzzer-disable").addEventListener("click", () => {
        gameSocket.sendCloseBuzz(lobbyCode);
    });
    document.getElementById("host-show-question").addEventListener("click", () => {
        gameSocket.sendAction(lobbyCode, gameSocket.Actions.MARK_CORRECT);
    });
    document.getElementById("host-hide-question").addEventListener("click", () => {
        gameSocket.sendAction(lobbyCode, gameSocket.Actions.MARK_INCORRECT);
    });
    document.getElementById("host-next-question").addEventListener("click", () => {
        gameSocket.sendAction(lobbyCode, gameSocket.Actions.RESET);
    });
    document.getElementById("host-end-game").addEventListener("click", () => {
        if (window.confirm("End the game and close this lobby for everyone?")) {
            gameSocket.sendAction(lobbyCode, gameSocket.Actions.END_GAME);
        }
    });
}

function configureLeaveButton() {
    document.getElementById("leave-btn").addEventListener("click", () => {
        socket.send("/app/leave", {lobbyCode});
        gameSocket.disconnect();
        clearLobbyDetails();
        window.location.href = "/index.html";
    });

    document.getElementById("main-buzzer").addEventListener("click", () => {
        gameSocket.sendBuzz(lobbyCode);
    });
}

function applyGameState(state) {
    if (!state || state.lobbyCode !== lobbyCode) return;

    const players = Object.keys(state.scores || {}).sort();
    if (players.some(username => username !== hostUsername && !playerViews.has(username))) {
        layoutPlayers(players);
    }

    for (const [username, score] of Object.entries(state.scores || {})) {
        playerViews.get(username)?.updateScore(score);
    }

    renderBoard(state);
    renderQuestion(state);
    renderBuzzer(state);
    renderHostControls(state);

    document.getElementById("game-title").textContent = state.gameTitle || "Trivia Throne";
    document.title = `${state.gameTitle || "Trivia Throne"} - TriviaThrone`;
    document.getElementById("game-status").textContent =
        state.buzzWinner ? `${state.buzzWinner} buzzed first.` : stateLabels[state.state] || "Waiting for the host…";
}

function renderBoard(state, preserveSelection = false) {
    const board = document.getElementById("jeopardy-board");
    const questionDisplay = document.getElementById("question-display");
    const hasQuestion = Boolean(state.question);
    board.classList.toggle("hidden", hasQuestion);
    questionDisplay.classList.toggle("hidden", !hasQuestion);
    if (hasQuestion) return;

    if (!preserveSelection && Number.isInteger(state.curRoundIdx)) selectedRoundIdx = state.curRoundIdx;
    const roundIndex = selectedRoundIdx;
    const round = state.rounds?.[roundIndex];
    if (!round) return;

    document.getElementById("jeopardy-round-title").textContent = round.name;
    const roundSelector = document.getElementById("round-selector");
    roundSelector.replaceChildren();
    state.rounds.forEach((availableRound, index) => {
        const tab = document.createElement("button");
        tab.className = "secondary-btn";
        tab.textContent = availableRound.name;
        tab.disabled = !isHost || state.state !== "IDLE";
        tab.setAttribute("role", "tab");
        tab.setAttribute("aria-selected", String(index === roundIndex));
        tab.addEventListener("click", () => {
            selectedRoundIdx = index;
            renderBoard(state, true);
            gameSocket.sendAction(
                lobbyCode,
                gameSocket.Actions.SELECT_ROUND,
                {roundIdx: index}
            );
        });
        roundSelector.appendChild(tab);
    });
    const canvas = document.getElementById("jeopardy-canvas");
    canvas.replaceChildren();
    canvas.style.gridTemplateColumns = `repeat(${Math.max(round.categories.length, 1)}, minmax(0, 1fr))`;
    canvas.style.gridTemplateRows = `repeat(${round.questions.length + 1}, minmax(0, 1fr))`;

    round.categories.forEach(category => {
        const heading = document.createElement("div");
        heading.className = "j-category";
        heading.textContent = String(category ?? "");
        canvas.appendChild(heading);
    });

    round.questions.forEach((row, questionIndex) => {
        row.forEach((tile, categoryIndex) => {
            const cell = document.createElement("button");
            cell.className = "j-cell";
            cell.textContent = tile.used ? "✓" : tile.points;
            cell.disabled = !isHost || state.state !== "IDLE" || tile.used;
            if (tile.used) cell.classList.add("used");
            if (state.curRoundIdx === roundIndex
                && state.curCatIdx === categoryIndex
                && state.curQuestionIdx === questionIndex
                && state.state !== "IDLE") {
                cell.classList.add("selected");
            }
            cell.setAttribute(
                "aria-label",
                `${round.categories[categoryIndex]}, ${tile.points} points${tile.used ? ", used" : ""}`
            );
            cell.addEventListener("click", () => gameSocket.sendAction(
                lobbyCode,
                gameSocket.Actions.SELECT_QUESTION,
                {roundIdx: roundIndex, categoryIdx: categoryIndex, questionIdx: questionIndex}
            ));
            canvas.appendChild(cell);
        });
    });
}

function renderQuestion(state) {
    const question = state.question;
    if (!question) {
        if (lastQuestionKey !== null) questionManager.reset();
        lastQuestionKey = null;
        return;
    }

    const key = `${state.curRoundIdx}:${state.curCatIdx}:${state.curQuestionIdx}`;
    if (key !== lastQuestionKey) {
        questionManager.showQuestion(question);
        lastQuestionKey = key;
    }
}

function renderBuzzer(state) {
    const buzzerOpen = state.state === "BUZZ_OPEN" || state.state === "BUZZ_COLLECTING";
    const alreadyBuzzed = (state.buzzedPlayers || []).includes(currentUser);
    const selfView = playerViews.get(currentUser);
    selfView?.setBuzzEnabled(!isHost && buzzerOpen && !alreadyBuzzed);

    for (const [username, view] of playerViews) {
        view.setBuzzWinner(username === state.buzzWinner);
    }

    const buzzer = document.getElementById("main-buzzer");
    buzzer.textContent = isHost ? "HOST" : alreadyBuzzed && buzzerOpen ? "LOCKED" : "BUZZ";
}

function renderHostControls(state) {
    document.getElementById("host-buzzer-enable").disabled = state.state !== "QUESTION_SELECTED";
    document.getElementById("host-buzzer-disable").disabled =
        state.state !== "BUZZ_OPEN" && state.state !== "BUZZ_COLLECTING";
    document.getElementById("host-show-question").disabled = state.state !== "BUZZ_LOCKED";
    document.getElementById("host-hide-question").disabled = state.state !== "BUZZ_LOCKED";
    document.getElementById("host-next-question").disabled = state.state === "IDLE";
}
