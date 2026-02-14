export const token = localStorage.getItem("token");
export const payload = parseJwt(token);
export const currentUser = payload?.sub;

if (!token || !payload || payload.exp * 1000 < Date.now()) {
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