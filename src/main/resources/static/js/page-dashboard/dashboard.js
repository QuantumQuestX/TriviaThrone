const dashboardSection = document.getElementById("dashboard-section");
const accountSection = document.getElementById("account-section");

function setSectionVisible(section, visible) {
    section.style.display = visible ? "block" : "none";
}

document.getElementById("dashboard-btn").addEventListener("click", () => {
    setSectionVisible(dashboardSection, true);
    setSectionVisible(accountSection, false);
});

document.getElementById("account-btn").addEventListener("click", () => {
    setSectionVisible(dashboardSection, false);
    setSectionVisible(accountSection, true);
});

const params = new URLSearchParams(window.location.search);
if (params.get("page") === "account") {
    setSectionVisible(dashboardSection, false);
    setSectionVisible(accountSection, true);
}