export class QuestionEditor {
    constructor(panelEl, resourceManager, onSaveCallback) {
        this.panel = panelEl;
        this.rm = resourceManager;
        this.onSave = onSaveCallback;

        this.form = this.panel.querySelector('#question-form');
        this.closeBtn = this.panel.querySelector('#close-editor-btn');
        this.choicesList = this.panel.querySelector('#choices-list');
        this.addChoiceBtn = this.panel.querySelector('#add-choice-btn');
        this.typeSelect = this.panel.querySelector('#question-type');
        this.mediaGroup = this.panel.querySelector('#media-group');
        this.mediaSelect = this.panel.querySelector('#question-media');

        this.attach();
        this.meta = null;
    }

    attach() {
        this.closeBtn.addEventListener('click', () => this.hide());

        this.form.addEventListener('submit', (e) => {
            e.preventDefault();
            this.save();
        });

        this.addChoiceBtn.addEventListener('click', (e) => {
            e.preventDefault();
            this.addChoice('', false);
        });

        this.typeSelect.addEventListener('change', () => this.updateVisibility());
    }

    open(round, category, question, meta) {
        this.meta = meta;
        this.panel.style.display = 'flex';

        this.panel.querySelector('#cat-display').value = category.title || '';
        this.panel.querySelector('#question-points').value = (question?.points) || ((meta.valueIdx + 1) * 100);
        this.typeSelect.value = (question && question.type) || 'text';
        this.panel.querySelector('#question-prompt').value = (question && question.prompt) || '';

        this.choicesList.innerHTML = '';
        if (question && question.choices) {
            question.choices.forEach(c => this.addChoice(c.text, !!c.correct));
        }

        if (this.mediaSelect) {
            this.mediaSelect.innerHTML = '<option value="">-- Select Resource --</option>';

            for (const [id, res] of this.rm.items) {
                const opt = document.createElement('option');
                opt.value = id;
                opt.textContent = res.name;
                this.mediaSelect.appendChild(opt);
            }

            if (question && question.media && question.media[0])
                this.mediaSelect.value = question.media[0];
        }

        this.updateVisibility();
    }

    hide() {
        this.panel.style.display = 'none';
        this.meta = null;
    }

    addChoice(text='', correct=false) {
        const div = document.createElement('div');
        div.className = 'choice-item';
        div.innerHTML = `<input type="checkbox" class="choice-correct" ${correct ? 'checked' : ''}>
          <input type="text" class="choice-text" value="${text}">
          <button type="button" class="choice-remove">x</button>`;

        div.querySelector('.choice-remove').addEventListener('click', () => div.remove());

        this.choicesList.appendChild(div);
    }

    updateVisibility() {
        const t = this.typeSelect.value;
        if (this.mediaGroup)
            this.mediaGroup.style.display = ['image', 'audio', 'video'].includes(t) ? 'block' : 'none';
        this.choicesList.parentElement.style.display = t === 'mcq' ? 'block' : 'none';
    }

    save() {
        if (!this.meta) return;

        const type = this.typeSelect.value;
        const prompt = this.panel.querySelector('#question-prompt').value.trim();
        const points = parseInt(this.panel.querySelector('#question-points').value, 10) || ((this.meta.valueIdx + 1) * 100);
        const media = this.mediaSelect ? (this.mediaSelect.value ? [this.mediaSelect.value] : []) : [];

        const choices = [];
        if (type === 'mcq') {
            this.choicesList.querySelectorAll('.choice-item').forEach(ci => {
                const text = ci.querySelector('.choice-text').value.trim();
                const correct = ci.querySelector('.choice-correct').checked;
                if (text)
                    choices.push({ text, correct });
            });
        }

        const question = {
            id: crypto.randomUUID(),
            type,
            prompt,
            points,
            choices, 
            media, 
            answered: false
        };

        const payload = { meta: this.meta, question };
        this.onSave('save', payload);
        this.hide();
    }
}