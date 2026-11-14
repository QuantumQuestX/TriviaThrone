import { requireSession } from "./auth.js";

export async function bootAuthenticatedPage(initialize) {
    try {
        if (!(await requireSession())) {
            return;
        }

        const topbar = await import("./topbar.js");

        topbar.initializeTopbarValues();
        await initialize();
    } catch (error) {
        console.error("Could not initialize the page.", error);

        const status = document.querySelector('[role="status"]') || document.createElement("p");

        status.setAttribute("role", "status");
        status.textContent = "The page could not be loaded. Check your connection and reload.";

        if (!status.isConnected) {
            document.body.prepend(status);
        }
    }
}
