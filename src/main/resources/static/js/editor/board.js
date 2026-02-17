import { Question } from "./question.js";

export class Board {
    constructor(container, onCellClickedCallback) {
        this.container = container;
        this.onCellClickedCallback = onCellClickedCallback;

        this.categoryCount = 1;
        this.questionCount = 1;
        this.categories = [{ name: "Category 1" }];
        
        this.questions = [];
        this.initQuestions();

        this.editingQuestion = null;
        this.appliedQuestions = new Set();
    }

    initQuestions() {
        this.questions = Array.from({ length: this.questionCount }, (_, r) =>
            Array.from({ length: this.categoryCount }, (_, c) => 
                new Question(c, r, (r + 1) * 100)
            )
        );
    }

    setCounts(newCategoryCount, newQuestionCount) {
        const oldQuestions = this.questions;
        const oldCategories = this.categories;

        this.categoryCount = newCategoryCount;
        this.questionCount = newQuestionCount;

        for (let i = 0; i < newCategoryCount; i++) {
            if (!this.categories[i])
                this.categories[i] = { name: `Category ${i + 1}` };
        }

        const newQuestions = Array.from({ length: newQuestionCount }, (_, r) =>
            Array.from({ length: newCategoryCount }, (_, c) =>
                oldQuestions[r]?.[c] || new Question(c, r, (r + 1) * 100)
            )
        );

        this.questions = newQuestions;

        this.appliedQuestions = new Set(
            [...this.appliedQuestions].filter(q =>
                q.rowIdx < this.questionCount && q.categoryIdx < this.categoryCount
        )
    );

        this.render();
    }

    renameCategory(index, newName) {
        if (index >= 0 && index < this.categories.length) {
            this.categories[index].name = newName;
            this.render();
        }
    }

    setQuestion(rowIdx, categoryIdx, { prompt, type, media, points }) {
        if (rowIdx > 0 && rowIdx < this.questionCount &&
            categoryIdx >= 0 && categoryIdx < this.categoryCount
        ) {
            const question = this.questions[rowIdx][categoryIdx];
            if (prompt !== undefined) question.setPrompt(prompt);
            if (type !== undefined) question.setType(type);
            if (media !== undefined) question.setMedia(media);
            if (points !== undefined) question.setPoints(points);
            this.renderCell(rowIdx, categoryIdx);
        }
    }

    render() {
        const catCount = this.categoryCount;
        const qCount = this.questionCount;

        this.container.innerHTML = '';
        this.container.style.display = 'grid';
        this.container.style.gridTemplateColumns = `repeat(${catCount}, 1fr)`;
        this.container.style.gridTemplateRows = `repeat(${qCount}, 1fr)`;

        for (let c = 0; c < catCount; c++) {
            const cat = document.createElement('div');
            cat.className = `j-category`;
            cat.textContent = this.categories[c]?.name || `Category ${c + 1}`;

            cat.addEventListener('click', () => {
                const name = prompt("Enter category name:", cat.textContent);
                if (name) {
                    this.renameCategory(c, name);
                }
            });

            this.container.appendChild(cat);
        }

        for (let r = 0; r < qCount; r++) {
            for (let c = 0; c < catCount; c++) {
                this.renderCell(r, c);
            }
        }
    }

    renderCell(rowIdx, categoryIdx) {
        const isOutOfBounds = rowIdx >= this.questionCount || categoryIdx >= this.categoryCount;
        if (isOutOfBounds) return;

        const cell = document.createElement('div');
        const question = this.questions[rowIdx][categoryIdx];

        cell.className = "j-cell";
        cell.textContent = question.points;

        if (this.editingQuestion === question) {
            cell.classList.add('editing');
        } else if (this.appliedQuestions.has(question)) {
            cell.classList.add('applied');
        } else {
            cell.classList.add('empty');
        }

        cell.addEventListener('click', () => {
            this.editingQuestion = question;
            this.render();
            this.onCellClickedCallback(question);
        });

        this.container.appendChild(cell);
    }

    applyQuestion(question) {
        this.appliedQuestions.add(question);
        this.deselectCell();
        this.render();
    }

    deselectCell() {
        this.editingQuestion = null;
        this.render();
    }
}