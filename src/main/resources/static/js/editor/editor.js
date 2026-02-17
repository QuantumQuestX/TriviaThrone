import { Board } from "./board.js";
import { Round } from "./round.js";
import { QuestionEditor } from "./question-editor.js";
import { token } from "./../root/token.js";

function toggleSidePanel(panelId, toState = null) {
    const panel = document.getElementById(panelId);
    if (!panel) return;
    if (toState !== null) {
        if (toState) {
            panel.classList.remove('collapsed');
        } else {
            panel.classList.add('collapsed');
        }
    } else {
        panel.classList.toggle('collapsed');
    }

    return panel.classList.contains('collapsed')
}

window.addEventListener('DOMContentLoaded', () => {
    console.log("Editor initialised.");

    const jeopardyCanvas = document.getElementById('jeopardy-canvas');
    const board = new Board(jeopardyCanvas, cellClicked);
    const questionEditor = new QuestionEditor(document.getElementById("question-editor-panel"));

    const QUESTION_EDITOR_SIDE_PANEL = 'right-panel';

    document.getElementById('left-panel-fold-btn').addEventListener('click', () => toggleSidePanel('left-panel'));
    document.getElementById('right-panel-fold-btn').addEventListener('click', () => {
        if (toggleSidePanel(QUESTION_EDITOR_SIDE_PANEL)) {
            questionEditor.close();
        }
    });

    const game = { rounds: [] };
    let currentRoundIdx = -1;

    const roundNameInput = document.getElementById('round-name');
    const roundSelect = document.getElementById('round-select');
    const addRoundBtn = document.getElementById('add-round-btn');
    const removeRoundBtn = document.getElementById('remove-round-btn');

    function refreshRoundSelect() {
        roundSelect.innerHTML = '';
        game.rounds.forEach((round, idx) => {
            const option = document.createElement('option');
            option.value = idx;
            option.textContent = round.name;
            roundSelect.appendChild(option);
        })
        if (currentRoundIdx >= 0) roundSelect.value = currentRoundIdx;
    }

    addRoundBtn.addEventListener('click', () => {
        const roundNumber = game.rounds.length + 1;

        let name = `Round ${roundNumber}`;
        if (roundNameInput.value.trim()) {
            name = roundNameInput.value.trim();
            roundNameInput.value = '';
        }

        const round = new Round(name);
        round.board = board;
        game.rounds.push(round);
        currentRoundIdx = game.rounds.length - 1;
        refreshRoundSelect();
        board.setCounts(round.categoryCount, round.questionCount);
    });

    removeRoundBtn.addEventListener('click', () => {
        if (currentRoundIdx < 0) return;
        game.rounds.splice(currentRoundIdx, 1);
        currentRoundIdx = game.rounds.length ? Math.max(0, currentRoundIdx - 1) : -1;
        refreshRoundSelect();
        if (currentRoundIdx >= 0) {
            const round = game.rounds[currentRoundIdx];
            board.setCounts(round.categoryCount, round.questionCount);
        } else {
            board.setCounts(0, 0);
        }
    });

    roundSelect.addEventListener('change', (e) => {
        currentRoundIdx = parseInt(e.target.value, 10);
        const round = game.rounds[currentRoundIdx];
        board.setCounts(round.categoryCount, round.questionCount);
    });

    const minCountValue = 1, maxCountValue = 6;
    const minCategoryCount = 1, maxCategoryCount = 5;

    const questionInc = document.getElementById('question-count-inc');
    const questionDec = document.getElementById('question-count-dec');
    const categoryInc = document.getElementById('category-count-inc');
    const categoryDec = document.getElementById('category-count-dec');

    const questionLabel = document.getElementById('question-count-text');
    const categoryLabel = document.getElementById('category-count-text');

    function updateCountLabels() {
        if (currentRoundIdx < 0) return;
        const round = game.rounds[currentRoundIdx];
        questionLabel.textContent = round.questionCount;
        categoryLabel.textContent = round.categoryCount;
        board.setCounts(round.categoryCount, round.questionCount);
    }
    updateCountLabels();

    questionInc.addEventListener('click', () => {
        const round = game.rounds[currentRoundIdx];
        if (round.questionCount < maxCountValue) {
            round.questionCount++;
            updateCountLabels();
        }
    });
    questionDec.addEventListener('click', () => {
        const round = game.rounds[currentRoundIdx];
        if (round.questionCount > minCountValue) {
            round.questionCount--;
            updateCountLabels();
        }
    });

    categoryInc.addEventListener('click', () => {
        const round = game.rounds[currentRoundIdx];
        if (round.categoryCount < maxCategoryCount) {
            round.categoryCount++;
            updateCountLabels();
        }
    });
    categoryDec.addEventListener('click', () => {
        const round = game.rounds[currentRoundIdx];
        if (round.categoryCount > minCategoryCount) {
            round.categoryCount--;
            updateCountLabels();
        }
    });

    function cellClicked(question) {
        toggleSidePanel(QUESTION_EDITOR_SIDE_PANEL, true);
        questionEditor.open(question);
    }
});
