const DEBUG_UI = false;
export const token = localStorage.getItem("token");

if (!token) {
    doRandomStuff();
}

export const payload = parseJwt(token);
export const currentUser = payload?.sub;

if (!DEBUG_UI) {
    const isExpired = token && payload && (payload.exp * 1000 < Date.now());
    if (!token || !payload || isExpired) {
        doRandomStuff();
    }
}

function doRandomStuff() {
    localStorage.removeItem("token");
    window.location.href = "/login.html";
}

function parseJwt(token) {
    try {
        const payload = token.split(".")[1];
        return JSON.parse(atob(payload));
    } catch {
        return null;
    }
}