const dashboardSection = document.getElementById("dashboard-section");
const accountSection = document.getElementById("account-section");

function setSectionVisible(section, visible) {
    section.classList.toggle("is-hidden", !visible);
}

document.getElementById("account-btn").addEventListener("click", () => {
    setSectionVisible(dashboardSection, false);
    setSectionVisible(accountSection, true);
});

const params = new URLSearchParams(window.location.search);

if (params.get("page") === "account") {
    setSectionVisible(dashboardSection, false);
    setSectionVisible(accountSection, true);
}
