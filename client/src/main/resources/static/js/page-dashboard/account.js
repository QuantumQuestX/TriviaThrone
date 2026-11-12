import {apiFetch} from "../root/api.js";
import {logout} from "../root/auth.js";

document.getElementById("change-password-form").addEventListener("submit", async(e) => {
    const form = e.target;
    e.preventDefault();

    const current = document.getElementById("current-password").value;
    const newPassword = document.getElementById("new-password").value;
    const confirmPassword = document.getElementById("confirm-password").value;
    const message = document.getElementById("password-message");

    message.className = "tt-status";
    message.textContent = "";

    if (newPassword !== confirmPassword) {
        message.textContent = "New passwords do not match.";
        message.classList.add("tt-status--error", "error");
        return;
    }

    if (newPassword.length < 8) {
        message.textContent = "Password must be at least 8 chars long";
        message.classList.add("tt-status--error", "error");
        return;
    }

    const submitButton = form.querySelector('button[type="submit"]');
    submitButton.disabled = true;

    try {
        const response = await apiFetch("/auth/change-password", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({
                oldPassword: current,
                newPassword
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "Password change failed");
        }

        message.textContent = "Password updated successfully";
        message.classList.add("tt-status--success", "success");
        form.reset();

        await logout();
        window.location.replace("/login");
    } catch (error) {
        message.textContent = error.message;
        message.classList.add("tt-status--error", "error");
        submitButton.disabled = false;
    }
});