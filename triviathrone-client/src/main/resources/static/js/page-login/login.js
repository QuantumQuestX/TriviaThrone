import { login } from "../root/auth.js";

document.getElementById("login-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const form = event.currentTarget;
    const submitButton = form.querySelector('button[type="submit"]');
    const errorMessage = document.getElementById("login-error");

    errorMessage.textContent = "";
    submitButton.disabled = true;

    try {
        await login(document.getElementById("username").value.trim(), document.getElementById("password").value);
        window.location.replace("/");
    } catch (error) {
        errorMessage.textContent = error.message;
        submitButton.disabled = false;
    }
});
