const versionDisplay = document.getElementById("app-version");

fetch("/api/version", { headers: { Accept: "text/plain" } })
    .then((response) => {
        if (!response.ok) {
            throw new Error(`Version request failed with status ${response.status}`);
        }

        return response.text();
    })
    .then((version) => {
        const currentVersion = version.trim();

        if (!currentVersion) {
            throw new Error("Version response was empty");
        }

        versionDisplay.textContent = `Version ${currentVersion}`;
    })
    .catch((error) => {
        console.error("Could not load the application version:", error);
        versionDisplay.textContent = "Version unavailable";
    });
