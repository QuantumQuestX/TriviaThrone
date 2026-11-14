const ACCESS_TOKEN_REFRESH_WINDOW_MS = 60_000;
const REFRESH_LOCK_NAME = "trivia-throne-refresh";

let accessToken = null;
let tokenPayload = null;
let expiresAt = 0;
let refreshTimer = null;
let refreshPromise = null;

export class AuthenticationError extends Error {
    constructor(message = "Your session has expired. Please sign in again.") {
        super(message);
        this.name = "AuthenticationError";
    }
}

export async function login(username, password) {
    const response = await fetch("/auth/login", {
        method: "POST",
        credentials: "same-origin",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
    });

    if (!response.ok) {
        throw new Error("Invalid username or password.");
    }

    return updateSession(await response.json());
}

export async function requireSession() {
    if (accessToken && expiresAt > Date.now() + ACCESS_TOKEN_REFRESH_WINDOW_MS) {
        return true;
    }

    try {
        await refreshAccessToken();

        return true;
    } catch (error) {
        if (error instanceof AuthenticationError) {
            redirectToLogin();

            return false;
        }

        throw error;
    }
}

export async function getAccessToken() {
    if (accessToken && expiresAt > Date.now() + ACCESS_TOKEN_REFRESH_WINDOW_MS) {
        return accessToken;
    }

    try {
        return await refreshAccessToken();
    } catch (error) {
        if (error instanceof AuthenticationError) {
            redirectToLogin();
        }

        throw error;
    }
}

export function getCurrentUser() {
    return tokenPayload?.sub ?? null;
}

export function getAccessTokenExpiresAt() {
    return expiresAt;
}

export function isAccessTokenNearExpiry() {
    return !accessToken || expiresAt <= Date.now() + ACCESS_TOKEN_REFRESH_WINDOW_MS;
}

export async function refreshAccessToken() {
    if (refreshPromise) {
        return refreshPromise;
    }

    const refresh = async () => {
        const response = await fetch("/auth/refresh", {
            method: "POST",
            credentials: "same-origin",
            headers: { Accept: "application/json" },
        });

        if (response.status === 401) {
            clearSession();
            throw new AuthenticationError();
        }

        if (!response.ok) {
            throw new Error("Could not refresh your session.");
        }

        return updateSession(await response.json());
    };

    refreshPromise = (async () => {
        if (globalThis.navigator?.locks) {
            return await navigator.locks.request(REFRESH_LOCK_NAME, { mode: "exclusive" }, refresh);
        }

        return await refresh();
    })().finally(() => {
        refreshPromise = null;
    });

    return refreshPromise;
}

export async function logout() {
    const response = await fetch("/auth/logout", {
        method: "POST",
        credentials: "same-origin",
    });

    if (!response.ok) {
        throw new Error("Could not end the session. Please try again.");
    }

    clearSession();
}

export function clearSession() {
    accessToken = null;
    tokenPayload = null;
    expiresAt = 0;
    window.clearTimeout(refreshTimer);
}

export function redirectToLogin() {
    if (window.location.pathname !== "/login") {
        window.location.replace("/login");
    }
}

function updateSession(data) {
    if (typeof data?.token !== "string") {
        clearSession();
        throw new Error("The server returned an invalid access token.");
    }

    const payload = decodeTokenPayload(data.token);

    if (!payload || typeof payload.sub !== "string" || !Number.isFinite(payload.exp)) {
        clearSession();
        throw new Error("The server returned an invalid access token.");
    }

    accessToken = data.token;
    tokenPayload = payload;
    expiresAt = payload.exp * 1000;
    scheduleRefresh();

    return accessToken;
}

function decodeTokenPayload(token) {
    try {
        const encodedPayload = token.split(".")[1];

        if (!encodedPayload) {
            return null;
        }

        const base64 = encodedPayload.replace(/-/g, "+").replace(/_/g, "/");
        const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
        const bytes = Uint8Array.from(atob(padded), (character) => character.charCodeAt(0));

        return JSON.parse(new TextDecoder().decode(bytes));
    } catch {
        return null;
    }
}

function scheduleRefresh() {
    window.clearTimeout(refreshTimer);

    const delay = Math.max(1000, expiresAt - Date.now() - ACCESS_TOKEN_REFRESH_WINDOW_MS);

    refreshTimer = window.setTimeout(async () => {
        try {
            await refreshAccessToken();
        } catch (error) {
            if (error instanceof AuthenticationError) {
                redirectToLogin();

                return;
            }

            console.error("Could not refresh the access token.", error);
            refreshTimer = window.setTimeout(scheduleRefresh, 30_000);
        }
    }, delay);
}
