import { payload } from "./token.js";

function initTopbarUserByToken() {
    const usernameSpan = document.getElementById("username-display");
    if (!usernameSpan) return;
    
    usernameSpan.textContent = payload?.sub || "Unknown";
}

initTopbarUserByToken();