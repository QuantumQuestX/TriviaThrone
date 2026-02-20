const DEBUG_UI = true;
export const token = localStorage.getItem("token");
export const payload = parseJwt(token);
export const currentUser = payload?.sub;

if (!DEBUG_UI) {
    const isExpired = token && payload && (payload.exp * 1000 < Date.now());
    if (isExpired) {
        localStorage.removeItem("token");
        window.location.href = "/login.html";
    }
}

function parseJwt(token) {
    try {
        const payload = token.split(".")[1];
        return JSON.parse(atob(payload));
    } catch {
        return null;
    }
}