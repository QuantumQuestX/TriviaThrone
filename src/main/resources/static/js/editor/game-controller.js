import { Round } from "./round.js";

export class GameController {
    constructor(boardController) {
        this.boardController = boardController;
        this.game = { rounds: [] };
        this.currentRoundIdx = -1;

        // Elements
        this.roundNameInput = document.getElementById('round-name');
        this.roundSelect = document.getElementById('round-select');
        this.addRoundBtn = document.getElementById('add-round-btn');
        this.removeRoundBtn = document.getElementById('remove-round-btn');

        this.questionInc = document.getElementById('question-count-inc');
        this.questionDec = document.getElementById('question-count-dec');
        this.categoryInc = document.getElementById('category-count-inc');
        this.categoryDec = document.getElementById('category-count-dec');
        this.questionLabel = document.getElementById('question-count-text');
        this.categoryLabel = document.getElementById('category-count-text');

        // Constants
        this.minQuestionCount = 1;
        this.maxQuestionCount = 6;

        this.minCategoryCount = 1;
        this.maxCategoryCount = 5;
    }

    init() {
        this.addRoundBtn.addEventListener('click', () => this.addRound());
        this.removeRoundBtn.addEventListener('click', () => this.removeRound());
        this.roundSelect.addEventListener('change', (e) => this.changeRound(e));

        this.questionInc.addEventListener('click', () => this.modifyQuestionCount(1));
        this.questionDec.addEventListener('click', () => this.modifyQuestionCount(-1));
        this.categoryInc.addEventListener('click', () => this.modifyCategoryCount(1));
        this.categoryDec.addEventListener('click', () => this.modifyCategoryCount(-1));

        this.refreshRoundSelect();
        this.updateCountLabels();
    }

    refreshRoundSelect()  {
        this.roundSelect.innerHTML = '';
        this.game.rounds.forEach((round, idx) => {
            const option = document.createElement('option');
            option.value = idx;
            option.textContent = round.name;
            this.roundSelect.appendChild(option);
        });
        if (this.currentRoundIdx >= 0) this.roundSelect.value = this.currentRoundIdx;
    }

    addRound() {
        const roundNumber = this.game.rounds.length + 1;
        let name = this.roundNameInput.value.trim() || `Round ${roundNumber}`;
        this.roundNameInput.value = '';

        const round = new Round(name);
        round.board = this.boardController.board;
        this.game.rounds.push(round);
        this.currentRoundIdx = this.game.rounds.length - 1;

        this.refreshRoundSelect();
        this.updateCountLabels();
    }

    removeRound() {
        if (this.currentRoundIdx < 0) return;
        this.game.rounds.splice(this.currentRoundIdx, 1);
        this.currentRoundIdx = this.game.rounds.length ? Math.max(0, this.currentRoundIdx - 1) : -1;
        this.refreshRoundSelect();
        this.updateCountLabels();
    }

    changeRound(e) {
        this.currentRoundIdx = parseInt(e.target.value, 10);
        this.updateCountLabels();
    }

    modifyQuestionCount(delta) {
        const round = this.game.rounds[this.currentRoundIdx];
        if (!round) return;
        round.questionCount = Math.min(this.maxQuestionCount, Math.max(this.minQuestionCount, round.questionCount + delta));
        this.updateCountLabels();
    }

    modifyCategoryCount(delta) {
        const round = this.game.rounds[this.currentRoundIdx];
        if (!round) return;
        round.categoryCount = Math.min(this.maxCategoryCount, Math.max(this.minCategoryCount, round.categoryCount + delta));
        this.updateCountLabels();
    }

    updateCountLabels() {
        if (this.currentRoundIdx < 0) return;

        const round = this.game.rounds[this.currentRoundIdx];

        if (!round) {
            this.questionLabel.textContent = '0';
            this.categoryLabel.textContent = '0';
            this.boardController.setCounts(0, 0);
            return;
        }

        this.questionLabel.textContent = round.questionCount;
        this.categoryLabel.textContent = round.categoryCount;
        this.boardController.setCounts(round.categoryCount, round.questionCount);
    }
}