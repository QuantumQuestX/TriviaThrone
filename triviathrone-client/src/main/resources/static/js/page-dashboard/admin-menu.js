import { apiFetch } from "../root/api.js";

const nav = document.querySelector(".site-header__actions");

if (nav && !nav.querySelector("[data-admin-link]")) {
    try {
        const response = await apiFetch("/api/admin/users");

        if (response.ok) {
            await response.json();

            const link = document.createElement("a");

            link.className = "tt-button tt-button--secondary";
            link.href = "/admin";
            link.textContent = "Admin";
            link.dataset.adminLink = "";
            nav.insertBefore(link, document.getElementById("account-btn"));
        }
    } catch (error) {
        console.error("Could not check admin access.", error);
    }
}
