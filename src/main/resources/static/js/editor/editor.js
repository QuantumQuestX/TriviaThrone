import { Board, Question } from "./board.js";
import { Round } from "./round.js";
import { token } from "./../root/token.js";


window.addEventListener('DOMContentLoaded', () => {
    console.log("Editor initialised.");

    const questionEditorSidePanel = "right-panel";

    function toggleSidePanel(panelId, toState = null) {
        const panel = document.getElementById(panelId);
        if (!panel) return;
        if (toState !== null) {
            if (toState)
                panel.classList.remove('collapsed');
            else
                panel.classList.add('collapsed');
        } else {
            panel.classList.toggle('collapsed');
        }
    }

    document.getElementById('left-panel-fold-btn').addEventListener('click', () => toggleSidePanel('left-panel'));
    document.getElementById('right-panel-fold-btn').addEventListener('click', () => toggleSidePanel(questionEditorSidePanel));

    const jeopardyCanvas = document.getElementById('jeopardy-canvas');
    const board = new Board(jeopardyCanvas, cellClicked);

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
            board.setCounts(1, 1);
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

    const questionCategoryInp = document.getElementById("question-category-input");
    const questionPromptInp = document.getElementById("question-prompt-input");
    const questionPointsInp = document.getElementById("question-points-input");
    const questionTypeSelect = document.getElementById("question-type-select");
    const questionMediaSelect = document.getElementById("question-media-select");
    const questionChoiceContainer = document.getElementById("question-choice-container");

    const questionMediaSection = document.getElementById("question-media-settings");
    const questionChoiceSection = document.getElementById("question-choice-settings");

    const questionAddChoiceBtn = document.getElementById("question-add-choice-btn");
    const questionApplyBtn = document.getElementById("question-editor-apply-btn");

    function cellClicked(question) {
        toggleSidePanel(questionEditorSidePanel, true);

        console.log("before cell clicked");
        questionCategoryInp.value = question.category ? question.category : '';
        questionPromptInp.value = question.prompt ? question.prompt : '';
        questionPointsInp.value = question.points ? question.points : '';
        questionTypeSelect.value = question.type ? question.type : '';
        questionTypeSelect.disabled = false;

        if (question.type === 'text') {
            hideNonTextQuestionOptions();
        } else if ((['image', 'video', 'audio']).includes(question.type)) {
            questionMediaSection.style.visibility = 'visible';
            questionChoiceSection.style.visibility = 'hidden';
        } else if (question.type === 'mcq') {
            questionMediaSection.style.visibility = 'hidden';
            questionChoiceSection.style.visibility = 'visible';
            generateQuestionChoices(question.options);
        }
    }

    questionTypeSelect.addEventListener('change', (e) => {
        console.log(e.target.value);
        if (e.target.value === 'text') {
            hideNonTextQuestionOptions();
        } else if ((['image', 'video', 'audio']).includes(e.target.value)) {
            questionMediaSection.style.visibility = 'visible';
            questionChoiceSection.style.visibility = 'hidden';
        } else if (e.target.value === 'mcq') {
            questionMediaSection.style.visibility = 'hidden';
            questionChoiceSection.style.visibility = 'visible';
        }
    });

    questionMediaSelect.addEventListener('change', (e) => {
        console.log(e.target.value);
    });

    function hideNonTextQuestionOptions() {
        questionMediaSection.style.visibility = 'hidden';
        questionChoiceSection.style.visibility = 'hidden';
    }

    function generateQuestionChoices(questionOptions) {
        if (!questionOptions) return;

        questionChoiceContainer.innerHTML = '';
        questionOptions.forEach((text, correct) => {
            addQuestionChoice(text, correct);
        });
    }

    function addQuestionChoice(text = '', checked = false) {
        const choice = document.createElement('div');
        choice.className = 'question-choice';

        questionChoiceContainer.appendChild(choice);

        const inputAnswer = document.createElement('input');
        inputAnswer.type = 'checkbox';
        inputAnswer.checked = checked;

        const inputText = document.createElement('input');
        inputText.type = 'text';
        inputText.value = text;

        const removeButton = document.createElement('button');
        removeButton.textContent = 'x';
        removeButton.className = 'danger-btn';

        removeButton.addEventListener('click', () => {
            questionChoiceContainer.removeChild(choice);
        });

        choice.appendChild(inputAnswer);
        choice.appendChild(inputText);
        choice.appendChild(removeButton);
    }

    questionAddChoiceBtn.addEventListener('click', () => {
        addQuestionChoice();
    });

    questionApplyBtn.addEventListener('click', () => {
        hideNonTextQuestionOptions();
        questionTypeSelect.disabled = true;
    });
});
