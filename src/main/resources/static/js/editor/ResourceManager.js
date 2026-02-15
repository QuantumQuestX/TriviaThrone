export class ResourceManager {
    constructor(maxSize=10*1024*1024, allowedMimes = ['image/png', 'audio/mpeg', 'video/mp4']) {
        this.items = new Map();
        this.maxSize = maxSize;
        this.allowedMimes = allowedMimes;
    }

    async addFile(file) {
        if (file.size > this.maxSize) throw new Error("File too large");
        if (!this.allowedMimes.includes(file.type)) throw new Error("Unsupported file type");
        
        const id = crypto.randomUUID();
        const url = URL.createObjectURL(file);
        const hash = await this._sha256(file);
        this.items.set(id, { id, name: file.name, mime: file.type, size: file.size, blob: file, url, hash });
        return id;
    }

    remove(id) {
        const it = this.items.get(id);
        if (it) {
            URL.revokeObjectURL(it.url);
            this.items.delete(id);
        }
    }

    async _sha256(file) {
        const buf = await file.arrayBuffer();
        const hashBuf = await crypto.subtle.digest('SHA-256', buf);
        return Array.from(new Uint8Array(hashBuf)).map(b => b.toString(16).padStart(2, '0')).join('');
    }
}