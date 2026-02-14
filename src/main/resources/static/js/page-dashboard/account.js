document.getElementById("change-password-form").addEventListener("submit", async(e) => {
    const form = e.target;
    e.preventDefault();

    const current = document.getElementById("current-password").value;
    const newPass = document.getElementById("new-password").value;
    const confirmPass = document.getElementById("confirm-password").value;
    const message = document.getElementById("password-message");

    message.className = "";
    message.textContent = "";

    if (newPass !== confirmPass) {
        message.textContent = "New passwords do not match.";
        message.classList.add("error");
        return;
    }

    if (newPass.length < 8) {
        message.textContent = "Password must be at least 8 chars long";
        message.classList.add("error");
        return;
    }

    try {
        const token = localStorage.getItem("token");
        if (!token) {
            message.textContent = "You are not logged in.";
            message.classList.add("error");
            return;
        }

        const response = await fetch("/auth/change-password", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Authorization": "Bearer " + token
            },
            body: JSON.stringify({
                oldPassword: current,
                newPassword: newPass
            })
        });

        if (!response.ok) {
            const errorText = await response.text();
            throw new Error(errorText || "Password change failed");
        }

        message.textContent = "Password updated successfully";
        message.classList.add("success");
        form.reset();

        localStorage.removeItem("token");
        window.location.href ="/login.html";
    } catch (error) {
        message.textContent = error.message;
        message.classList.add("error");
    }
});