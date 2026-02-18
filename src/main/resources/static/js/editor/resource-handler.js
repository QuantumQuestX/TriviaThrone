export class ResourceHandler {
    constructor(resourceManager) {
        this.ResourceManager = resourceManager;
        this.mediaSelect = document.getElementById('question-media-select');
    }

    init() {
        const fileInput = document.getElementById('game-file');
        const uploadBox = document.getElementById('upload-box');
        const resourceList = document.getElementById('resource-list');
        const status = document.getElementById('upload-status');

        uploadBox.addEventListener('click', () => {
            fileInput.click();
        });

        fileInput.addEventListener('change', (e) => {
            this.handleFiles(e.target.files, resourceList, status);
            fileInput.value = '';
        });

        this.initDragAndDrop(uploadBox, resourceList, status);
    }

    handleFiles(fileList, container, status) {
        const files = Array.from(fileList);
        let added = 0;
        
        for (const file of files) {
            const resource = this.ResourceManager.addFile(file);
            this.renderResource(resource, container);
            added++;
        }

        status.textContent = `${added} file(s) added.`;
    }

    initDragAndDrop(uploadBox, container, status) {
        const preventDefaults = (e) => {
            e.preventDefault();
            e.stopPropagation();
        };

        ['dragenter', 'dragover', 'dragleave', 'drop']
            .forEach(eventName => {
                uploadBox.addEventListener(eventName, preventDefaults, false);
            });

        uploadBox.addEventListener('dragenter', () => {
            uploadBox.classList.add('drag-over');
        });

        uploadBox.addEventListener('dragover', () => {
            uploadBox.classList.add('drag-over');
        });

        uploadBox.addEventListener('dragleave', () => {
            uploadBox.classList.remove('drag-over');
        });

        uploadBox.addEventListener('drop', (e) => {
            uploadBox.classList.remove('drag-over');
            const files = e.dataTransfer.files;
            this.handleFiles(files, container, status);
        });
    }

    renderResource(resource, container) {
        const item = document.createElement('div');
        item.className = 'resource-item';
        item.dataset.id = resource.id;

        const preview = document.createElement('div');
        preview.className = 'resource-preview';

        if (resource.type === 'image') {
            const img = document.createElement('img');
            img.src = URL.createObjectURL(resource.file);
            preview.appendChild(img);
        } else {
            const icon = document.createElement('div');
            icon.className = 'resource-icon';
            icon.textContent =
                resource.type === 'video' ? '' :
                resource.type === 'audio' ? '' :
                '';
            preview.appendChild(icon);
        }

        const meta = document.createElement('div');
        meta.className = 'resource-meta';

        const name = document.createElement('div');
        name.className = 'resource-name';
        name.textContent = resource.name;

        const id = document.createElement('div');
        id.className = 'resource-id mono';
        id.textContent = resource.id;

        meta.append(name, id);
        item.append(preview, meta);

        item.addEventListener('click', () => {
            item.remove();
        });

        container.appendChild(item);

        this.updateMediaSelect(resource);
    }

    updateMediaSelect(resource) {
        const option = document.createElement('option');
        option.value = resource.id;
        option.textContent = `${resource.name} (${resource.type})`;

        this.mediaSelect.appendChild(option);
    }
}