document.getElementById("login-form").addEventListener("submit", async e => {
    e.preventDefault();
    const username = document.getElementById("username").value;
    const password = document.getElementById("password").value;

    try {
        const res = await fetch("/auth/login", {
           method: "POST",
           headers: { "Content-Type": "application/json" },
           body: JSON.stringify({ username, password })
        });

        if (!res.ok) {
            throw new Error("Invalid username or password");
        }

        const data = await res.json();
        localStorage.setItem("token", data.token);
        window.location.href = "/";
    } catch (err) {
        document.getElementById("login-error").textContent = err.message;
    }
});