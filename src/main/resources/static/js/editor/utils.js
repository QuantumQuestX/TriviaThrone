export class StorageManager {
    constructor(key) {
        this.key = key;
    }

    save(obj) {
        localStorage.setItem(this.key, JSON.stringify(obj));
    }

    load() {
        const s = localStorage.getItem(this.key);
        return s ? JSON.parse(s) : null;
    }

    clear() {
        localStorage.removeItem(this.key);
    }
}