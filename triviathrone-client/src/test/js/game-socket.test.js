import assert from "node:assert/strict";
import {afterEach, test} from "node:test";

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

const payload = Buffer.from(JSON.stringify({
    sub: "host",
    exp: Math.floor(Date.now() / 1000) + 3600
})).toString("base64url");
const token = `header.${payload}.signature`;
const originalFetch = globalThis.fetch;
const sentMessages = [];
const subscribedTopics = [];
const client = {
    connected: false,
    connect(headers, onConnect) {
        assert.equal(headers.Authorization, `Bearer ${token}`);
        this.connected = true;
        onConnect();
    },
    subscribe(topic) {
        subscribedTopics.push(topic);
        return {unsubscribe() {}};
    },
    send(destination, headers, body) {
        sentMessages.push({destination, headers, body});
    },
    disconnect() {
        this.connected = false;
    }
};

globalThis.SockJS = class {
    constructor(endpoint) {
        this.endpoint = endpoint;
    }
};
globalThis.Stomp = {
    over(socket) {
        assert.equal(socket.endpoint, "/ws/lobby");
        return client;
    }
};
globalThis.fetch = async url => {
    if (url !== "/auth/refresh") throw new Error(`Unexpected fetch: ${url}`);
    return new Response(JSON.stringify({token}), {
        status: 200,
        headers: {"Content-Type": "application/json"}
    });
};

const {clearSession} = await import("../../main/resources/static/js/root/auth.js");
const gameSocket = await import(
    "../../../../triviathrone-games/jeopardy/src/main/resources/ui/js/game/game-socket.js"
);

afterEach(() => {
    gameSocket.disconnect();
    clearSession();
    globalThis.fetch = originalFetch;
    sentMessages.length = 0;
    subscribedTopics.length = 0;
});

test("host media action is sent to the game action destination", async () => {
    await gameSocket.subscribeToGame("ROOM01", () => {});

    gameSocket.sendAction("ROOM01", gameSocket.Actions.PLAY_MEDIA);

    assert.deepEqual(subscribedTopics, ["/topic/game.ROOM01"]);
    assert.equal(sentMessages.length, 1);
    assert.equal(sentMessages[0].destination, "/app/game/action");
    assert.deepEqual(JSON.parse(sentMessages[0].body), {
        lobbyCode: "ROOM01",
        action: "PLAY_MEDIA",
        parameters: {}
    });
});
