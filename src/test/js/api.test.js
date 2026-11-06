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

function createToken(username, expirationOffsetSeconds = 3600) {
    const payload = Buffer.from(JSON.stringify({
        sub: username,
        exp: Math.floor(Date.now() / 1000) + expirationOffsetSeconds
    })).toString("base64url");
    return `header.${payload}.signature`;
}

const originalFetch = globalThis.fetch;
const {clearSession, login} = await import("../../main/resources/static/js/root/auth.js");
const {apiFetch} = await import("../../main/resources/static/js/root/api.js");

afterEach(() => {
    clearSession();
    globalThis.fetch = originalFetch;
});

test("refreshes and retries a 401 even when the token is not near expiry", async () => {
    const initialToken = createToken("player");
    const refreshedToken = createToken("player", 3601);
    const requestTokens = [];
    let refreshCount = 0;

    globalThis.fetch = async (resource, options = {}) => {
        if (resource === "/auth/login") {
            return Response.json({token: initialToken});
        }
        if (resource === "/auth/refresh") {
            refreshCount += 1;
            return Response.json({token: refreshedToken});
        }

        requestTokens.push(options.headers.get("Authorization"));
        return requestTokens.length === 1
            ? new Response("Unauthorized", {status: 401})
            : new Response("OK", {status: 200});
    };

    await login("player", "password");
    const response = await apiFetch("/api/game/state");

    assert.equal(response.status, 200);
    assert.equal(refreshCount, 1);
    assert.deepEqual(requestTokens, [
        `Bearer ${initialToken}`,
        `Bearer ${refreshedToken}`
    ]);
});
