const GLOBAL_QUESTION_CHOICES_COUNT = 4;

export class QuestionEditor {
    constructor(rootElement) {
        this.root = rootElement;

        this.categoryInput = rootElement.querySelector('#question-category-input');
        this.promptInput = rootElement.querySelector('#question-prompt-input');
        this.pointsInput = rootElement.querySelector('#question-points-input');
        this.typeSelect = rootElement.querySelector('#question-type-select');

        this.mediaSection = rootElement.querySelector('#question-media-settings');
        this.mediaSelect = rootElement.querySelector('#question-media-select');

        this.choiceSection = rootElement.querySelector('#question-choice-settings');
        this.choiceContainer = rootElement.querySelector('#question-choice-container');
        this.addChoiceBtn = rootElement.querySelector('#question-add-choice-btn');

        this.applyBtn = rootElement.querySelector('#question-editor-apply-btn');

        this.currentQuestion = null;

        this.initEvents();
        this.disable();
    }

    initEvents() {
        this.typeSelect.addEventListener('change', () => {
            this.updateVisibility();
        });

        this.addChoiceBtn.addEventListener('click', () => {
            this.addChoice();
        });

        this.applyBtn.addEventListener('click', () => {
            this.apply();
        });
    }

    open(question) {
        this.currentQuestion = question;

        this.enable();
        this.load(question);
    }

    close() {
        this.currentQuestion = null;
        this.disable();
        this.clear();
    }

    enable() {
        this.root.querySelectorAll('input, select, button:not(.fold-btn)')
            .forEach(el => el.disabled = false);
    }

    disable() {
        this.root.querySelectorAll('input, select, button:not(.fold-btn)')
            .forEach(el => el.disabled = true);
    }

    load(question) {
        this.categoryInput.value = question.categoryIdx + 1;
        this.promptInput.value = question.prompt || '';
        this.pointsInput.value = question.points;
        this.typeSelect.value = question.type;

        this.updateVisibility();

        if (question.type === 'mcq' && question.options) {
            this.renderChoices(question.options);
        }
    }

    clear() {
        this.promptInput.value = '';
        this.pointsInput.value = '';
        this.typeSelect.value = 'text';
        this.choiceContainer.innerHTML = '';
    }

    updateVisibility() {
        const type = this.typeSelect.value;

        ['image', 'video', 'audio'].includes(type) 
                ? this.mediaSection.classList.remove('is-hidden')
                : this.mediaSection.classList.add('is-hidden');

        type === 'mcq' 
                ? this.choiceSection.classList.remove('is-hidden')
                : this.choiceSection.classList.add('is-hidden')
    }

    renderChoices(options) {
        this.choiceContainer.innerHTML = '';
        options.forEach(opt => {
            this.addChoice(opt.text, opt.correct);
        });
    }

    addChoice(text='', correct=false) {
        if (this.choiceContainer.childElementCount >= GLOBAL_QUESTION_CHOICES_COUNT) return;

        const row = document.createElement('div');
        row.className = 'question-choice';

        const checkbox = document.createElement('input');
        checkbox.type = 'checkbox';
        checkbox.checked = correct;

        const input = document.createElement('input');
        input.type = 'text';
        input.value = text;

        const remove = document.createElement('button');
        remove.textContent = 'x';
        remove.className = 'danger-btn';
        remove.type = 'button';

        remove.addEventListener('click', () => row.remove());

        row.append(checkbox, input, remove);
        this.choiceContainer.appendChild(row);
    }

    collectChoices() {
        return [...this.choiceContainer.children].map(row => ({
            text: row.querySelector('input[type="text"]').value,
            correct: row.querySelector('input[type="checkbox"]').checked
        }));
    }

    apply() {
        if (!this.currentQuestion) return;

        this.currentQuestion.setPrompt(this.promptInput.value);
        this.currentQuestion.setPoints(parseInt(this.pointsInput.value, 10));
        this.currentQuestion.setType(this.typeSelect.value);

        if (this.typeSelect.value === 'mcq') {
            this.currentQuestion.setOptions(this.collectChoices());
        }

        if (['image', 'video', 'audio'].includes(this.typeSelect.value)) {
            this.currentQuestion.setMedia(this.mediaSection.value);
        }
    }
}