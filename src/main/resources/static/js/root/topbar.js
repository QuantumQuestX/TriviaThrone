import { payload } from "./token.js";

export function initTopbarUserByToken() {
    const usernameSpan = document.getElementById("username-display");
    if (!usernameSpan) return;
    
    usernameSpan.textContent = payload?.sub || "Unknown";
}

initTopbarUserByToken();