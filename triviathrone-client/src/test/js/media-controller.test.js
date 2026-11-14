import assert from "node:assert/strict";
import {afterEach, beforeEach, test} from "node:test";

class FakeClassList {
    constructor(...classes) {
        this.classes = new Set(classes);
    }

    add(...classes) {
        classes.forEach(value => this.classes.add(value));
    }

    remove(...classes) {
        classes.forEach(value => this.classes.delete(value));
    }

    contains(value) {
        return this.classes.has(value);
    }
}

class FakeElement {
    constructor(...classes) {
        this.classList = new FakeClassList(...classes);
        this.textContent = "";
        this.children = [];
        this._innerHTML = "";
        this.src = "";
        this.paused = true;
        this.playCount = 0;
        this.pauseCount = 0;
    }

    set innerHTML(value) {
        this._innerHTML = value;
        this.children = [];
    }

    get innerHTML() {
        return this._innerHTML;
    }

    appendChild(child) {
        this.children.push(child);
        return child;
    }

    play() {
        this.playCount += 1;
        this.paused = false;
        return Promise.resolve();
    }

    pause() {
        this.pauseCount += 1;
        this.paused = true;
    }
}

const originalFetch = globalThis.fetch;
let elements;
let mediaRequests;
let mediaResponse;
let accessToken;

globalThis.window = {
    clearTimeout() {},
    setTimeout() {
        return 1;
    },
    location: {
        pathname: "/game.html",
        replace() {}
    }
};

globalThis.document = {
    getElementById(id) {
        return elements[id];
    },
    createElement() {
        return new FakeElement();
    }
};

const {clearSession} = await import("../../main/resources/static/js/root/auth.js");
const {MediaController, MediaType} = await import(
    "../../../../triviathrone-games/jeopardy/src/main/resources/ui/js/game/jeopardy/media-controller.js"
);
const {QuestionManager} = await import(
    "../../../../triviathrone-games/jeopardy/src/main/resources/ui/js/game/jeopardy/question-manager.js"
);

beforeEach(() => {
    const payload = Buffer.from(JSON.stringify({
        sub: "test-player",
        exp: Math.floor(Date.now() / 1000) + 3600
    })).toString("base64url");
    accessToken = `header.${payload}.signature`;
    elements = {
        "q-media-video": new FakeElement("hidden"),
        "q-media-audio": new FakeElement("hidden"),
        "q-media-image": new FakeElement("hidden"),
        "media-frame": new FakeElement("hidden"),
        "media-placeholder": new FakeElement("hidden"),
        "media-placeholder-text": new FakeElement(),
        "question-category": new FakeElement(),
        "question-points": new FakeElement(),
        "question-text": new FakeElement(),
        "question-choices": new FakeElement("hidden")
    };
    mediaRequests = [];
    mediaResponse = async () => new Response(
        new Blob(["media bytes"], {type: "video/mp4"}),
        {status: 200}
    );
    clearSession();
    globalThis.fetch = async (url, options = {}) => {
        if (url === "/auth/refresh") {
            return new Response(JSON.stringify({token: accessToken}), {
                status: 200,
                headers: {"Content-Type": "application/json"}
            });
        }
        mediaRequests.push({url, authorization: options.headers.get("Authorization")});
        return mediaResponse(url, options);
    };
});

afterEach(() => {
    clearSession();
    globalThis.fetch = originalFetch;
});

test("video stays on a host-start placeholder until playback begins", async () => {
    const controller = new MediaController();
    await controller.setMedia(MediaType.VIDEO, "/api/game/ROOM01/media?path=resources%2Fclip.mp4");

    assert.equal(elements["media-frame"].classList.contains("hidden"), false);
    assert.equal(elements["media-placeholder"].classList.contains("hidden"), false);
    assert.match(elements["media-placeholder-text"].textContent, /Waiting for the host/);
    assert.equal(elements["q-media-video"].classList.contains("hidden"), true);
    assert.equal(elements["q-media-video"].playCount, 0);
    assert.equal(mediaRequests[0].authorization, `Bearer ${accessToken}`);
});

test("video playback replaces the placeholder and buzzing restores it", async () => {
    const controller = new MediaController();
    await controller.setMedia(MediaType.VIDEO, "/api/game/ROOM01/media?path=resources%2Fclip.mp4");

    await controller.play();
    assert.equal(elements["q-media-video"].classList.contains("hidden"), false);
    assert.equal(elements["media-placeholder"].classList.contains("hidden"), true);

    await controller.pause();
    assert.equal(elements["q-media-video"].paused, true);
    assert.equal(elements["q-media-video"].classList.contains("hidden"), true);
    assert.equal(elements["media-placeholder"].classList.contains("hidden"), false);
    assert.match(elements["media-placeholder-text"].textContent, /Video paused/);
});

test("audio placeholder reports playback and buzz pause", async () => {
    const controller = new MediaController();
    await controller.setMedia(MediaType.AUDIO, "/api/game/ROOM01/media?path=resources%2Fclue.mp3");

    await controller.play();
    assert.match(elements["media-placeholder-text"].textContent, /Audio is playing/);

    await controller.pause();
    assert.equal(elements["q-media-audio"].paused, true);
    assert.match(elements["media-placeholder-text"].textContent, /Audio paused/);
});

test("image questions do not fail when a buzz pause event arrives", async () => {
    const controller = new MediaController();
    await controller.setMedia(MediaType.IMAGE, "/api/game/ROOM01/media?path=resources%2Fimage.png");

    assert.equal(elements["q-media-image"].classList.contains("hidden"), false);
    assert.equal(elements["media-frame"].classList.contains("hidden"), true);
    await controller.pause();
});

test("a buzz cancels a playback request that is still loading", async () => {
    let resolveResponse;
    mediaResponse = () => new Promise(resolve => {
        resolveResponse = resolve;
    });

    const controller = new MediaController();
    const loadPromise = controller.setMedia(
        MediaType.VIDEO,
        "/api/game/ROOM01/media?path=resources%2Fclip.mp4"
    );
    const playPromise = controller.play();
    const pausePromise = controller.pause();

    while (!resolveResponse) {
        await new Promise(resolve => setImmediate(resolve));
    }
    resolveResponse(new Response(new Blob(["media"], {type: "video/mp4"}), {status: 200}));
    await Promise.all([loadPromise, playPromise, pausePromise]);

    assert.equal(elements["q-media-video"].playCount, 0);
    assert.match(elements["media-placeholder-text"].textContent, /Video paused/);
});

test("QuestionManager waits for authenticated media loading before reporting question ready", async () => {
    const manager = new QuestionManager();
    await manager.showQuestion({
        category: "Audio",
        points: 200,
        prompt: "Name the song",
        mediaType: MediaType.AUDIO,
        mediaUrl: "/api/game/ROOM01/media?path=resources%2Fclue.mp3"
    });

    assert.equal(elements["question-category"].textContent, "Audio");
    assert.equal(elements["question-points"].textContent, 200);
    assert.equal(elements["question-text"].textContent, "Name the song");
    assert.match(elements["media-placeholder-text"].textContent, /Waiting for the host/);
});

test("QuestionManager renders multiple-choice options and reveals the answer only when told", async () => {
    const manager = new QuestionManager();
    const questionChoices = elements["question-choices"];

    await manager.showQuestion({
        category: "Geography",
        points: 200,
        prompt: "Which ocean is largest?",
        choices: ["Atlantic Ocean", "Pacific Ocean"],
        correctChoiceIndex: null
    });

    assert.equal(questionChoices.classList.contains("hidden"), false);
    assert.deepEqual(questionChoices.children.map(choice => choice.textContent), [
        "A. Atlantic Ocean",
        "B. Pacific Ocean"
    ]);
    assert.equal(questionChoices.children.some(choice => choice.classList.contains("correct")), false);

    manager.revealCorrectAnswer(1);

    assert.equal(questionChoices.children[0].classList.contains("wrong"), true);
    assert.equal(questionChoices.children[1].classList.contains("correct"), true);
});
