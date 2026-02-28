import * as gameSocket from "./game-socket.js";
import { PlayerView } from "./jeopardy/player-view.js";
import { currentUser } from "../root/token.js";
import { QuestionManager } from "./jeopardy/question-manager.js";

const titleContent = document.getElementsByTagName("title")[0].textContent || '{0} - Trivia Throne';
document.getElementsByTagName("title")[0].textContent = titleContent.replace("{0}", new URLSearchParams(window.location.search).get("mode"));

const lobbyDetails = JSON.parse(localStorage.getItem("lobbyDetails")) || "{}";
const lobbyCode = lobbyDetails.lobbyCode;

gameSocket.subscribeToGame(lobbyCode, (state) => {
    applyGameState(state);
});

const playerViews = new Map();
const questionManager = new QuestionManager();

function layoutPlayers(players, currentUser) {
    const left = document.getElementById("players-left");
    const right = document.getElementById("players-right");

    left.innerHTML = "";
    right.innerHTML = "";

    const others = players.filter(p => p !== currentUser);

    const mid = Math.ceil(others.length / 2);

    const leftSide = others.slice(0, mid);
    const rightSide = others.slice(mid);

    leftSide.forEach(player => {
        const view = new PlayerView(player);
        view.mount(left);
        playerViews.set(player, view);
    });

    rightSide.forEach(player => {
        const view = new PlayerView(player);
        view.mount(right);
        playerViews.set(player, view);
    });

    const selfView = new PlayerView(currentUser, { isSelf: true });
    playerViews.set(currentUser, selfView);
}

function configureMainBuzzer(currentUser) {
    const mainBuzzer = document.getElementById("main-buzzer");
    
    mainBuzzer.addEventListener("click", () => {
        console.log(`${currentUser} buzzed in!`);
        gameSocket.sendBuzz(currentUser);

        mainBuzzer.disabled = true;
    });
}

function applyGameState(state) {
    if (state.question) {
        document.getElementById("jeopardy-board").classList.add("hidden");
        document.getElementById("question-display").classList.remove("hidden");

        questionManager.showQuestion(state.question);
    } else {
        document.getElementById("question-display").classList.add("hidden");
        document.getElementById("jeopardy-board").classList.remove("hidden");
    }

    if (state.revealCorrectIndex !== undefined && state.revealCorrectIndex !== null) {
        questionManager.revealCorrectAnswer(state.revealCorrectIndex);
    }

    for (const [name, score] of Object.entries(state.scores)) {
        const view = playerViews.get(name);
        if (view) view.updateScore(score);
    }

    const buzzOpen = state.state === gameSocket.Actions.OPEN_BUZZ;
    const selfView = playerViews.get(currentUser);
    if (selfView) {
        selfView.setBuzzEnabled(buzzOpen);
    }
}

function generateJeopardyBoard(boardMatrix) {
    const canvas = document.getElementById("jeopardy-canvas");

    const categories = boardMatrix[0];
    const rows = boardMatrix.slice(1);

    const columnCount = categories.length;
    const rowCount = rows.length + 1;

    canvas.style.gridTemplateColumns = `repeat(${columnCount}, 1fr)`;
    canvas.style.gridTemplateRows = `repeat(${rowCount}, 1fr)`;

    for (let col = 0; col < columnCount; col++) {
        const categoryCell = document.createElement("div");
        categoryCell.classList.add("j-category");
        categoryCell.textContent = categories[col];
        canvas.appendChild(categoryCell);
        console.log(`Added category: ${categories[col]}`);
    }

    for (let row = 0; row < rows.length; row++) {
        for (let col = 0; col < columnCount; col++) {
            const cell = document.createElement("div");
            cell.classList.add("j-cell");
            cell.textContent = rows[row][col];
            canvas.appendChild(cell);
        }
    }
}

generateJeopardyBoard([
    ["Hallo", "Welt", "Foo", "Bar", "Sayo"],
    [100, 200, 300, 400, 500],
    [100, 200, 300, 200, 500],
    [100, 200, 300, 400, 500],
    [100, 200, 300, 400, 500],
    [100, 200, 300, 400, 500],
    [100, 200, 300, 400, 500]
]);


const currentUser2 = "Alice"; // This would be dynamically set in a real application
layoutPlayers([currentUser, "Bob", "Charlie", "Diana", "Peter", "Sven"], currentUser);
configureMainBuzzer(currentUser);