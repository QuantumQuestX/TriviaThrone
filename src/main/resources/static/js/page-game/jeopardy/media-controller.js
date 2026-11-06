import {apiFetch} from "../../root/api.js";

export const MediaType = Object.freeze({
    VIDEO: "VIDEO",
    AUDIO: "AUDIO",
    IMAGE: "IMAGE",
    UNDEFINED: "UNDEFINED" 
});

export class MediaController {
    constructor() {
        this.videoElement = document.getElementById("q-media-video");
        this.audioElement = document.getElementById("q-media-audio");
        this.imageElement = document.getElementById("q-media-image");
        this.type = null;
        this.loadId = 0;
        this.playbackVersion = 0;
        this.objectUrl = null;
        this.mediaLoadPromise = Promise.resolve();
    }

    setMedia(type = MediaType.UNDEFINED, url) {
        if (!type || type === MediaType.UNDEFINED || !url) {
            console.warn("No media to display");
            return Promise.resolve();
        }

        this.clearMedia();
        this.type = type;
        const loadId = this.loadId;
        this.mediaLoadPromise = this.loadMedia(type, url, loadId);
        return this.mediaLoadPromise;
    }

    clearMedia() {
        this.loadId += 1;
        this.playbackVersion += 1;
        this.mediaLoadPromise = Promise.resolve();
        if (this.objectUrl) {
            URL.revokeObjectURL(this.objectUrl);
            this.objectUrl = null;
        }

        this.videoElement.pause();
        this.videoElement.src = "";
        this.videoElement.classList.add("hidden");

        this.audioElement.pause();
        this.audioElement.src = "";
        this.audioElement.classList.add("hidden");

        this.imageElement.src = "";
        this.imageElement.classList.add("hidden");
        this.type = null;
    }

    async loadMedia(type, url, loadId) {
        const response = await apiFetch(url);
        if (loadId !== this.loadId) return;
        if (!response.ok) {
            throw new Error(`Could not load question media (${response.status})`);
        }

        const mediaBlob = await response.blob();
        if (loadId !== this.loadId) return;

        const element = this.getElement(type);
        if (!element) {
            throw new Error(`Unsupported question media type: ${type}`);
        }
        this.objectUrl = URL.createObjectURL(mediaBlob);
        element.src = this.objectUrl;
        element.classList.remove("hidden");
    }

    getElement(type) {
        switch (type) {
            case MediaType.VIDEO:
                return this.videoElement;
            case MediaType.AUDIO:
                return this.audioElement;
            case MediaType.IMAGE:
                return this.imageElement;
            default:
                return null;
        }
    }

    play() {
        const playbackVersion = ++this.playbackVersion;
        return this.mediaLoadPromise.then(() => {
            if (playbackVersion !== this.playbackVersion) return;
            if (this.type === MediaType.VIDEO) {
                return this.videoElement.play();
            } else if (this.type === MediaType.AUDIO) {
                return this.audioElement.play();
            }
        });
    }

    pause() {
        const playbackVersion = ++this.playbackVersion;
        return this.mediaLoadPromise.then(() => {
            if (playbackVersion !== this.playbackVersion) return;
            if (this.type === MediaType.VIDEO) {
                this.videoElement.pause();
            } else if (this.type === MediaType.AUDIO) {
                this.audioElement.pause();
            }
        });
    }
}