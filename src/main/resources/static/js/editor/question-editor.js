const GLOBAL_QUESTION_CHOICES_COUNT = 4;
const GLOBAL_MEDIA_PLACEHOLDER_VALUE = '';

export class QuestionEditor {
    constructor(rootElement, onApplyCallback, resourceManager) {
        this.root = rootElement;
        this.onApplyCallback = onApplyCallback;
        this.resourceManager = resourceManager;

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

        const mediaTypes = ['image', 'video', 'audio'];
        mediaTypes.includes(type) 
                ? this.mediaSection.classList.remove('is-hidden')
                : this.mediaSection.classList.add('is-hidden');

        type === 'mcq' 
                ? this.choiceSection.classList.remove('is-hidden')
                : this.choiceSection.classList.add('is-hidden')

        if (mediaTypes.includes(type)) {
            this.updateMediaOptions(type);
        }
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

    updateMediaOptions(type) {
        const resources = this.resourceManager.getAll();
        this.mediaSelect.innerHTML = '';

        const placeholder = document.createElement('option');
        placeholder.value = GLOBAL_MEDIA_PLACEHOLDER_VALUE;
        placeholder.textContent = '-- Select Resource --';
        this.mediaSelect.appendChild(placeholder);

        resources
            .filter(r => r.type === type)
            .forEach(r => {
                const option = document.createElement('option');
                option.value = r.id;
                option.textContent = `${r.name} (${r.type})`;
                this.mediaSelect.appendChild(option);
            });

        if (this.currentQuestion && this.currentQuestion.media && this.currentQuestion.media.type === type) {
            this.mediaSelect.value = this.currentQuestion.media.id;
        }
    }

    apply() {
        if (!this.currentQuestion) return;

        const promptValue = this.promptInput.value.trim();
        if (!promptValue) {
            alert("Question prompt cannot be empty.");
            this.promptInput.focus();
            return;
        }

        this.currentQuestion.setPrompt(this.promptInput.value);
        this.currentQuestion.setPoints(parseInt(this.pointsInput.value, 10));
        this.currentQuestion.setType(this.typeSelect.value);

        if (this.typeSelect.value === 'mcq') {
            const collectedChoices = this.collectChoices();

            const validChoices = collectedChoices.filter(c => c.text.trim() !== '');
            if (validChoices.length < 1 || validChoices.length !== collectedChoices.length) {
                alert("Multiple Choice question cannot have no or empty choices.");

                if (collectedChoices.length > 0)
                    this.choiceContainer.querySelector('input[type="text"]').focus();
                else
                    this.addChoiceBtn.focus();

                return;
            }

            this.currentQuestion.setOptions(collectedChoices);
        }

        if (['image', 'video', 'audio'].includes(this.typeSelect.value)) {
            const mediaValue = this.mediaSelect.value;

            if (!mediaValue || mediaValue === GLOBAL_MEDIA_PLACEHOLDER_VALUE) {
                alert("Media Question cannot have no selected media");
                this.mediaSection.focus();
                return;
            }

            this.currentQuestion.setMedia(mediaValue);
        }

        if (this.onApplyCallback)
            this.onApplyCallback(this.currentQuestion);

        this.close();
    }
}