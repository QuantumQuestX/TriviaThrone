export class ResourcePanel {
    constructor(resourceManager, containerEl) {
        this.rm = resourceManager;
        this.container = containerEl;
        this.fileInput = this.container.querySelector('#resource-file');
        this.status = this.container.querySelector('#upload-status');
        this.list = this.container.querySelector('#resource-list');

        this.attach();
        this.render();
    }

    attach() {
        if (!this.fileInput) return;

        this.fileInput.addEventListener('change', (e) => this.handleFiles(e.target.files));
        const uploadBox = this.container.querySelector('.upload-box');

        uploadBox.addEventListener('drop', (e) => {
            e.preventDefault();
            this.handleFiles(e.dataTransfer.files);
        });

        uploadBox.addEventListener('dragover', (e) => e.preventDefault());
    }

    async handleFiles(files) {
        const errors = [];
        for (const f of files) {
            try {
                await this.rm.addFile(f);
            } catch (err) {
                errors.push(`${f.name}: ${err.message}`);
            }
        }

        this.status.textContent = errors.length ? errors.join("; ") : `Added ${files.length} file(s)`;
        this.render();
    }

    render() {
        this.list.innerHTML = '';

        for (const [id, m] of this.rm.items) {
            const div = document.createElement('div');
            div.className = 'resource-item';

            const thumb = m.mime.startsWith('image/') 
                ? `<img src="${m.url}" class="resource-thumbnail"/>`
                : `<div style="width: 48px; height: 48px; display: flex; align-items: center; justify-content: center;">${m.mime.startsWith('audio/')? '🔊' : m.mime.startsWith('video/')? '🎥' : '📁'}</div>`;

            div.innerHTML = `${thumb}
                <div class="resource-info">
                    <div class="resource-name">${m.name}</div>
                    <div class="resource-meta">${(m.size / 1024)|0} KB * ${m.mime}</div>
                </div>
                <button class="resource-remove" data-id="${id}">x</button>
            `;

            div.querySelector('.resource-remove').addEventListener('click', () => {
                this.rm.remove(id);
                this.status.textContent = `Removed ${m.name}`;
                this.render();
            });

            this.list.appendChild(div);
        }
    }
}