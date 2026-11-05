import {getAccessTokenExpiresAt, getCurrentUser} from "./auth.js";

function initTopbarUserByToken() {
    document.querySelectorAll("#username-display").forEach(element => {
        element.textContent = getCurrentUser() || "Unknown";
    });

    const expiryElement = document.getElementById("token-expiry");
    if (expiryElement) updateTokenExpiry(expiryElement);
}

function updateTokenExpiry(element) {
    const minutes = Math.max(0, Math.ceil((getAccessTokenExpiresAt() - Date.now()) / 60_000));
    element.textContent = minutes
        ? `${minutes} minute${minutes === 1 ? "" : "s"}`
        : "Refreshing…";
    window.setTimeout(() => updateTokenExpiry(element), 60_000);
}

initTopbarUserByToken();