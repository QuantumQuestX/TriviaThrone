const token = localStorage.getItem("token");

if (!token) {
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