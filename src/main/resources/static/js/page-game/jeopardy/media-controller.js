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
    }

    setMedia(type = MediaType.UNDEFINED, url) {
        if (!type || type === MediaType.UNDEFINED || !url) {
            console.warn("No media to display");
            return;
        }

        this.clearMedia();

        this.type = type;

        switch (type) {
            case MediaType.VIDEO:
                this.videoElement.src = url;
                this.videoElement.classList.remove("hidden");
                break;
            case MediaType.AUDIO:
                this.audioElement.src = url;
                this.audioElement.classList.remove("hidden");
                break;
            case MediaType.IMAGE:
                this.imageElement.src = url;
                this.imageElement.classList.remove("hidden");
                break;
            default:
                console.warn("Unknown media type:", type);
        }
    }

    clearMedia() {
        this.videoElement.pause();
        this.videoElement.src = "";
        this.videoElement.classList.add("hidden");

        this.audioElement.pause();
        this.audioElement.src = "";
        this.audioElement.classList.add("hidden");

        this.imageElement.src = "";
        this.imageElement.classList.add("hidden");
    }

    play() {
        if (this.type === MediaType.VIDEO) {
            this.videoElement.play();
        } else if (this.type === MediaType.AUDIO) {
            this.audioElement.play();
        }
    }
}