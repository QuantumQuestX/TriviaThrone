import {requireSession} from "./auth.js";

export async function bootAuthenticatedPage(initialize) {
    try {
        if (!await requireSession()) return;

        await import("./topbar.js");
        await initialize();
    } catch (error) {
        console.error("Could not initialize the page.", error);
        const status = document.querySelector('[role="status"]') || document.createElement("p");
        status.setAttribute("role", "status");
        status.textContent = "The page could not be loaded. Check your connection and reload.";
        if (!status.isConnected) document.body.prepend(status);
    }
}
